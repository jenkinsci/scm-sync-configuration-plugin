package hudson.plugins.scm_sync_configuration;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationFilter;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.*;
import hudson.plugins.scm_sync_configuration.transactions.AtomicTransaction;
import hudson.plugins.scm_sync_configuration.transactions.ScmTransaction;
import hudson.plugins.scm_sync_configuration.transactions.ThreadedTransaction;
import hudson.plugins.scm_sync_configuration.xstream.ScmSyncConfigurationXStreamConverter;
import hudson.plugins.scm_sync_configuration.xstream.migration.ScmSyncConfigurationPOJO;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.PluginServletFilter;
import net.sf.json.JSONObject;

import org.acegisecurity.AccessDeniedException;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.Nullable;
import javax.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

public class ScmSyncConfigurationPlugin extends Plugin{

    public static final transient ScmSyncStrategy[] AVAILABLE_STRATEGIES = new ScmSyncStrategy[]{
        new JenkinsConfigScmSyncStrategy(),
        new BasicPluginsConfigScmSyncStrategy(),
        new JobConfigScmSyncStrategy(),
        new UserConfigScmSyncStrategy(),
        new ManualIncludesScmSyncStrategy()
    };

    /**
     * Strategies that cannot be updated by user
     */
    public static final transient List<ScmSyncStrategy> DEFAULT_STRATEGIES = ImmutableList.copyOf(
            Collections2.filter(Arrays.asList(AVAILABLE_STRATEGIES), new Predicate<ScmSyncStrategy>() {
                @Override
                public boolean apply(@Nullable ScmSyncStrategy scmSyncStrategy) {
                    return !( scmSyncStrategy instanceof ManualIncludesScmSyncStrategy );
                }
            }));

    public void purgeFailLogs() {
        business.purgeFailLogs();
    }

    public static interface AtomicTransactionFactory {
        AtomicTransaction createAtomicTransaction();
    }

    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationPlugin.class.getName());

    private transient ScmSyncConfigurationBusiness business;

    /**
     * Flag allowing to process commit synchronously instead of asynchronously (default)
     * Could be useful, particularly during tests execution
     */

    private transient boolean synchronousTransactions = false;

    /**
     * SCM Transaction which is currently used. This transaction is thread scoped and will be, by default,
     * an AtomicTransaction (each time a change is recorded, it will be immediately commited).
     * Every time a transaction will be commited, it will be resetted to null
     */
    private transient ThreadLocal<ScmTransaction> transaction = new ThreadLocal<ScmTransaction>();

    private String scmRepositoryUrl;
    private SCM scm;
    private boolean noUserCommitMessage;
    private boolean displayStatus = true;
    // The [message] is a magic string that will be replaced with commit message
    // when commit occurs
    private String commitMessagePattern = "[message]";
    private List<File> filesModifiedByLastReload;
    private List<String> manualSynchronizationIncludes;

    public ScmSyncConfigurationPlugin(){
        // By default, transactions should be asynchronous
        this(false);
    }

    public ScmSyncConfigurationPlugin(boolean synchronousTransactions){
        this.synchronousTransactions = synchronousTransactions;
        setBusiness(new ScmSyncConfigurationBusiness());

        try {
            PluginServletFilter.addFilter(new ScmSyncConfigurationFilter());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getManualSynchronizationIncludes(){
        return manualSynchronizationIncludes;
    }

    @Override
    public void start() throws Exception {
        super.start();

        Jenkins.XSTREAM.registerConverter(new ScmSyncConfigurationXStreamConverter());

        this.load();

        // If scm has not been read in scm-sync-configuration.xml, let's initialize it
        // to the "no scm" SCM
        if(this.scm == null){
            this.scm = SCM.valueOf(ScmSyncNoSCM.class);
            this.scmRepositoryUrl = null;
        }

        // SCMManagerFactory.start() must be called here instead of ScmSyncConfigurationItemListener.onLoaded()
        // because, for some unknown reasons, we reach plexus bootstraping exceptions when
        // calling Embedder.start() when everything is loaded (very strange...)
        SCMManagerFactory.getInstance().start();
        initialInit();
    }

    public void loadData(ScmSyncConfigurationPOJO pojo){
        this.scmRepositoryUrl = pojo.getScmRepositoryUrl();
        this.scm = pojo.getScm();
        this.noUserCommitMessage = pojo.isNoUserCommitMessage();
        this.displayStatus = pojo.isDisplayStatus();
        this.commitMessagePattern = pojo.getCommitMessagePattern();
        this.manualSynchronizationIncludes = pojo.getManualSynchronizationIncludes();
        this.business.setManualSynchronizationIncludes(manualSynchronizationIncludes);
    }

    protected void initialInit() throws Exception {
        // We need to init() here in addition to ScmSyncConfigurationItemListener.onLoaded() to ensure that we do
        // indeed create the SCM work directory when we are loaded. Otherwise, the plugin can be installed but
        // then fails to operate until the next time Jenkins is restarted. Using postInitialize() for this might
        // be too late if the plugin is copied to the plugin directory and then Jenkins is started.
        this.business.init(createScmContext());
    }

    public void init() {
        try {
            this.business.init(createScmContext());
        } catch (Exception e) {
            throw new RuntimeException("Error during ScmSyncConfiguration initialisation !", e);
        }
    }

    @Override
    public void stop() throws Exception {
        SCMManagerFactory.getInstance().stop();
        super.stop();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData)
            throws IOException, ServletException, FormException {
        super.configure(req, formData);

        boolean repoInitializationRequired = false;
        boolean configsResynchronizationRequired = false;
        boolean repoCleaningRequired = false;

        this.noUserCommitMessage = formData.getBoolean("noUserCommitMessage");
        this.displayStatus = formData.getBoolean("displayStatus");
        this.commitMessagePattern = req.getParameter("commitMessagePattern");

        String oldScmRepositoryUrl = this.scmRepositoryUrl;
        String scmType = req.getParameter("scm");
        if(scmType != null){
            this.scm = SCM.valueOf(scmType);
            String newScmRepositoryUrl = this.scm.createScmUrlFromRequest(req);

            this.scmRepositoryUrl = newScmRepositoryUrl;

            // If something changed, let's reinitialize repository in working directory !
            repoInitializationRequired = newScmRepositoryUrl != null && !newScmRepositoryUrl.equals(oldScmRepositoryUrl);
            configsResynchronizationRequired = repoInitializationRequired;
            repoCleaningRequired = newScmRepositoryUrl==null && oldScmRepositoryUrl!=null;
        }

        if(req.getParameterValues("manualSynchronizationIncludes") != null){
            List<String> submittedManualIncludes = new ArrayList<String>(Arrays.asList(req.getParameterValues("manualSynchronizationIncludes")));
            List<String> newManualIncludes = new ArrayList<String>(submittedManualIncludes);
            if(this.manualSynchronizationIncludes != null){
                newManualIncludes.removeAll(this.manualSynchronizationIncludes);
            }
            this.manualSynchronizationIncludes = submittedManualIncludes;

            configsResynchronizationRequired = !newManualIncludes.isEmpty();
        } else {
            this.manualSynchronizationIncludes = new ArrayList<String>();
        }

        this.business.setManualSynchronizationIncludes(manualSynchronizationIncludes);

        // Repo initialization should be made _before_ plugin save, in order to let scm-sync-configuration.xml
        // file synchronizable
        if(repoInitializationRequired){
            this.business.initializeRepository(createScmContext(), true);
        }
        if(configsResynchronizationRequired){
            this.business.synchronizeAllConfigs(AVAILABLE_STRATEGIES);
        }
        if(repoCleaningRequired){
            this.business.cleanChekoutScmDirectory();
        }

        // Persisting plugin data
        // Note that save() is made _after_ the synchronizeAllConfigs() because, otherwise, scm-sync-configuration.xml
        // file would be commited _before_ every other jenkins configuration file, which doesn't seem "natural"
        this.save();
    }

    public Iterable<File> collectAllFilesForScm() {
        return Iterables.concat(Iterables.transform(Lists.newArrayList(AVAILABLE_STRATEGIES), new Function<ScmSyncStrategy, Iterable<File>>() {
            @Override
            public Iterable<File> apply(ScmSyncStrategy strategy) {
                return strategy.collect();
            }}));
    }

    public Iterable<File> collectAllFilesForScm(final File fromSubDirectory) {
        return Iterables.concat(Iterables.transform(Lists.newArrayList(AVAILABLE_STRATEGIES), new Function<ScmSyncStrategy, Iterable<File>>() {
            @Override
            public Iterable<File> apply(ScmSyncStrategy strategy) {
                return strategy.collect(fromSubDirectory);
            }}));
    }

    // public method for UI-less reload in e. g. Groovy scripts
    public void reloadAllFilesFromScm() throws ServletException, IOException {
        try {
            filesModifiedByLastReload = business.reloadAllFilesFromScm();
        }
        catch(ScmException e) {
            throw new ServletException("Unable to reload SCM " + scm.getTitle() + ":" + getScmUrl(), e);
        }
    }

    public void doReloadAllFilesFromScm(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
        try {
            filesModifiedByLastReload = business.reloadAllFilesFromScm();
            req.getView(this, "/hudson/plugins/scm_sync_configuration/reload.jelly").forward(req, res);
        }
        catch(ScmException e) {
            throw new ServletException("Unable to reload SCM " + scm.getTitle() + ":" + getScmUrl(), e);
        }
    }

    public void doSubmitComment(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
        // TODO: complexify this in order to pass a strategy identifier in the session key
        ScmSyncConfigurationDataProvider.provideComment(req.getParameter("comment"));
        if(Boolean.valueOf(req.getParameter("dontBotherMe")).booleanValue()){
            ScmSyncConfigurationDataProvider.provideBotherTimeout(req.getParameter("botherType"),
                    Integer.valueOf(req.getParameter("botherTime")), req.getParameter("currentURL"));
        }
    }

    // TODO: do retrieve help file with an action !
    public void doHelpForRepositoryUrl(StaplerRequest req, StaplerResponse res) throws ServletException, IOException{
        req.getView(this, SCM.valueOf(req.getParameter("scm")).getRepositoryUrlHelpPath()).forward(req, res);
    }

    // Help url for manualSynchronizationIncludes field is a jelly script and not a html file
    // because we need default includes list to be displayed in it !
    public void doManualIncludesHelp(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
        req.getView(this, "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/help/manualSynchronizationIncludes.jelly").forward(req, res);
    }

    public void doSynchronizeFile(@QueryParameter String path){
        getTransaction().registerPath(path);
    }

    /**
     * This method is invoked via jelly to display a list of all the default includes.
     *
     * @return a list of explanatory strings about the patterns matched by a specific strategy's matcher.
     */
    public List<String> getDefaultIncludes(){
        List<String> includes = new ArrayList<String>();
        for(ScmSyncStrategy strategy : DEFAULT_STRATEGIES){
            includes.addAll(strategy.getSyncIncludes());
        }
        return includes;
    }

    private User getCurrentUser(){
        User user = null;
        try {
            user = Jenkins.getInstance().getMe();
        }catch(AccessDeniedException e){}
        return user;
    }

    public static ScmSyncConfigurationPlugin getInstance(){
        return Jenkins.getInstance().getPlugin(ScmSyncConfigurationPlugin.class);
    }

    public ScmSyncStrategy getStrategyForSaveable(Saveable s, File f){
        for(ScmSyncStrategy strat : AVAILABLE_STRATEGIES){
            if(strat.isSaveableApplicable(s, f)){
                return strat;
            }
        }
        // Strategy not found !
        return null;
    }

    /**
     * Tries to find at least one strategy that would have applied to a deleted item.
     * 
     * @param s the saveable that was deleted. It still exists in Jenkins' model, but has already been eradicated from disk.
     * @param pathRelativeToRoot where the item had lived on disk
     * @param wasDirectory whether it was a directory
     * @return a strategy that thinks it might have applied
     */
    public ScmSyncStrategy getStrategyForDeletedSaveable(Saveable s, String pathRelativeToRoot, boolean wasDirectory) {
        for (ScmSyncStrategy strategy : AVAILABLE_STRATEGIES) {
            if (strategy.mightHaveBeenApplicableToDeletedSaveable(s, pathRelativeToRoot, wasDirectory)) {
                return strategy;
            }
        }
        return null;
    }

    public ScmContext createScmContext(){
        return new ScmContext(this.scm, this.scmRepositoryUrl, this.commitMessagePattern);
    }

    public boolean shouldDecorationOccursOnURL(String url){
        // Removing comment from session here...
        ScmSyncConfigurationDataProvider.retrieveComment(true);

        // Displaying commit message popup is based on following tests :
        // Zero : never ask for a commit message
        // First : no botherTimeout should match with current url
        // Second : a strategy should exist, matching current url
        // Third : SCM Sync should be settled up
        return !noUserCommitMessage && ScmSyncConfigurationDataProvider.retrieveBotherTimeoutMatchingUrl(url) == null
                && getStrategyForURL(url) != null && this.business.scmCheckoutDirectorySettledUp(createScmContext());
    }

    public ScmSyncStrategy getStrategyForURL(String url){
        for(ScmSyncStrategy strat : AVAILABLE_STRATEGIES){
            if(strat.isCurrentUrlApplicable(url)){
                return strat;
            }
        }
        // Strategy not found !
        return null;
    }

    public boolean isNoUserCommitMessage() {
        return noUserCommitMessage;
    }

    public SCM[] getScms(){
        return SCM.values();
    }

    public void setBusiness(ScmSyncConfigurationBusiness business) {
        this.business = business;
    }

    public ScmSyncConfigurationStatusManager getScmSyncConfigurationStatusManager() {
        return business.getScmSyncConfigurationStatusManager();
    }

    public String getScmRepositoryUrl() {
        return scmRepositoryUrl;
    }

    public boolean isScmSelected(SCM _scm){
        return this.scm == _scm;
    }

    public SCM getSCM(){
        return this.scm;
    }

    public String getScmUrl(){
        if(this.scm != null){
            return this.scm.extractScmUrlFrom(this.scmRepositoryUrl);
        } else {
            return null;
        }
    }

    public List<File> getFilesModifiedByLastReload() {
        return filesModifiedByLastReload;
    }

    public boolean isDisplayStatus() {
        return displayStatus;
    }

    public String getCommitMessagePattern() {
        return commitMessagePattern;
    }

    public Descriptor<? extends hudson.scm.SCM> getDescriptorForSCM(String scmName){
        return SCM.valueOf(scmName).getSCMDescriptor();
    }

    public void startThreadedTransaction(){
        this.setTransaction(new ThreadedTransaction(synchronousTransactions));
    }

    public Future<Void> commitChangeset(ChangeSet changeset){
        try {
            if(!changeset.isEmpty()){
                return this.business.queueChangeSet(createScmContext(), changeset, getCurrentUser(), ScmSyncConfigurationDataProvider.retrieveComment(false));
            } else {
                return null;
            }
        } finally {
            // Reinitializing transaction once commited
            this.setTransaction(null);
        }
    }

    public ScmTransaction getTransaction() {
        if(transaction.get() == null){
            setTransaction(new AtomicTransaction(synchronousTransactions));
        }
        return transaction.get();
    }

    protected void setTransaction(ScmTransaction transactionToRegister){
        if(transaction.get() != null && transactionToRegister != null){
            LOGGER.warning("Existing threaded transaction will be overriden !");
        }
        transaction.set(transactionToRegister);
    }

    public boolean currentUserCannotPurgeFailLogs() {
        return !business.canCurrentUserPurgeFailLogs();
    }

    private static final Pattern STARTS_WITH_DRIVE_LETTER = Pattern.compile("^[a-zA-Z]:");

    /**
     * UI form validation for the git repository URL. Must be non-empty, a valid URL, and git must be able to access the repository through it.
     * 
     * @param value from the UI form
     * @return the validation status, with possible error or warning messages.
     */
    public FormValidation doCheckGitUrl(@QueryParameter String value) {
        if (Strings.isNullOrEmpty(value)) {
            return FormValidation.error(Messages.ScmSyncConfigurationsPlugin_gitRepoUrlEmpty());
        }
        String trimmed = value.trim();
        // Plain file paths are valid URIs, except maybe on windows if starting with a drive letter
        if (!isValidUrl (trimmed)) {
            // We have two more possibilities:
            // - a plain file path starting with a drive letter and a colon on windows(?). Just delegate to the repository access below.
            // - a ssh-like short form like [user@]host.domain.tld:repository
            if (!STARTS_WITH_DRIVE_LETTER.matcher(trimmed).find()) {
                // Possible ssh short form?
                if (trimmed.indexOf("://") < 0 && trimmed.indexOf(':') > 0) {
                    if (!isValidUrl("ssh://" + trimmed.replaceFirst(":", "/"))) {
                        return FormValidation.error(Messages.ScmSyncConfigurationsPlugin_gitRepoUrlInvalid());
                    }
                } else {
                    return FormValidation.error(Messages.ScmSyncConfigurationsPlugin_gitRepoUrlInvalid());
                }
            }
        }
        // Try to access the repository...
        if (Jenkins.getInstance().hasPermission(Permission.CONFIGURE)) {
            try {
                ScmProvider scmProvider = SCMManagerFactory.getInstance().createScmManager().getProviderByUrl("scm:git:" + trimmed);
                // Stupid interface. Why do I have to pass a delimiter if the URL must already be without "scm:git:" prefix??
                ScmProviderRepository remoteRepo = scmProvider.makeProviderScmRepository(trimmed, ':');
                // File set and parameters are ignored by the maven SCM gitexe implementation (for now...)
                scmProvider.remoteInfo(remoteRepo, new ScmFileSet(Jenkins.getInstance().getRootDir()), new CommandParameters());
                // We actually don't care about the result. If this cannot access the repo, it'll raise an exception.
            } catch (ComponentLookupException e) {
                LOGGER.warning("Cannot validate repository URL: no ScmManager: " + e.getMessage());
                // And otherwise ignore
            } catch (ScmException e) {
                LOGGER.warning("Repository at " + trimmed + " is inaccessible");
                return FormValidation.error(Messages.ScmSyncConfigurationsPlugin_gitRepoUrlInaccessible(trimmed));
            }
        }
        if (trimmed.length() != value.length()) {
            return FormValidation.warning(Messages.ScmSyncConfigurationsPlugin_gitRepoUrlWhitespaceWarning());
        }
        return FormValidation.ok();
    }

    /**
     * Determines whether the given string is a valid URL.
     * 
     * @param input to check
     * @return {@code true} if the input string is a vlid URL, {@code false} otherwise.
     */
    private boolean isValidUrl(String input) {
        try {
            // There might be no "stream handler" in URL for ssh or git. We always replace the protocol by http for this check.
            String httpUrl = input.replaceFirst("^[a-zA-Z]+://", "http://");
            new URI(httpUrl).toURL();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (URISyntaxException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
