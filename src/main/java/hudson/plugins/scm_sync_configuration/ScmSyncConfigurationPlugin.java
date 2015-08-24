package hudson.plugins.scm_sync_configuration;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
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
import hudson.util.PluginServletFilter;
import net.sf.json.JSONObject;
import org.acegisecurity.AccessDeniedException;
import org.apache.maven.scm.ScmException;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Logger;

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
    public static final transient List<ScmSyncStrategy> DEFAULT_STRATEGIES = new ArrayList<ScmSyncStrategy>(){{
        addAll(Collections2.filter(Arrays.asList(AVAILABLE_STRATEGIES), new Predicate<ScmSyncStrategy>() {
            public boolean apply(@Nullable ScmSyncStrategy scmSyncStrategy) {
                return !( scmSyncStrategy instanceof ManualIncludesScmSyncStrategy );
            }
        }));
    }};

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

    private transient Future<Void> latestCommitFuture;

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

		Hudson.XSTREAM.registerConverter(new ScmSyncConfigurationXStreamConverter());

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
            // Cleaning checkouted repository
            this.business.cleanChekoutScmDirectory();
        }

        // Persisting plugin data
        // Note that save() is made _after_ the synchronizeAllConfigs() because, otherwise, scm-sync-configuration.xml
        // file would be commited _before_ every other jenkins configuration file, which doesn't seem "natural"
		this.save();
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
			user = Hudson.getInstance().getMe();
		}catch(AccessDeniedException e){}
		return user;
	}

	public static ScmSyncConfigurationPlugin getInstance(){
		return Hudson.getInstance().getPlugin(ScmSyncConfigurationPlugin.class);
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
                latestCommitFuture = this.business.queueChangeSet(createScmContext(), changeset, getCurrentUser(), ScmSyncConfigurationDataProvider.retrieveComment(false));
                return latestCommitFuture;
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

    public Future<Void> getLatestCommitFuture() {
        return latestCommitFuture;
    }
}
