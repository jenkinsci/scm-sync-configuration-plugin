package hudson.plugins.scm_sync_configuration;

import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JenkinsConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JobConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.xstream.ScmSyncConfigurationXStreamConverter;
import hudson.plugins.scm_sync_configuration.xstream.migration.ScmSyncConfigurationPOJO;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.acegisecurity.AccessDeniedException;
import org.apache.maven.scm.ScmException;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ScmSyncConfigurationPlugin extends Plugin{
	
	public static final transient ScmSyncStrategy[] AVAILABLE_STRATEGIES = new ScmSyncStrategy[]{
			new JobConfigScmSyncStrategy(),
			new JenkinsConfigScmSyncStrategy()
	};
	
	private transient ScmSyncConfigurationBusiness business;
	private String scmRepositoryUrl;
	private SCM scm;
	private boolean noUserCommitMessage;
	private boolean displayStatus;
    // The [message] is a magic string that will be replaced with commit message
    // when commit occurs
    private String commitMessagePattern = "[message]";

	public ScmSyncConfigurationPlugin(){
		setBusiness(new ScmSyncConfigurationBusiness());
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
		
		String scmType = req.getParameter("scm");
		if(scmType != null){
			this.noUserCommitMessage = formData.containsKey("noUserCommitMessage");
			this.displayStatus = formData.containsKey("displayStatus");
            this.commitMessagePattern = req.getParameter("commitMessagePattern");
			
			this.scm = SCM.valueOf(scmType);
			String newScmRepositoryUrl = this.scm.createScmUrlFromRequest(req);
			
			String oldScmRepositoryUrl = this.scmRepositoryUrl;
			this.scmRepositoryUrl = newScmRepositoryUrl;
			this.save();
			
			// If something changed, let's reinitialize repository in working directory !
			if(newScmRepositoryUrl != null && !newScmRepositoryUrl.equals(oldScmRepositoryUrl)){
				this.business.initializeRepository(createScmContext(), true);
				this.business.synchronizeAllConfigs(createScmContext(), AVAILABLE_STRATEGIES, getCurrentUser());
			} else if(newScmRepositoryUrl==null && oldScmRepositoryUrl!=null){
				// Cleaning checkouted repository
				this.business.cleanChekoutScmDirectory();
			}
		}
	}
	
	public List<File> reloadAllFilesFromScm() throws IOException, ScmException {
		return business.reloadAllFilesFromScm(AVAILABLE_STRATEGIES);
	}
	
	public void doSubmitComment(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
		// TODO: complexify this in order to pass a strategy identifier in the session key
		ScmSyncConfigurationDataProvider.provideComment(req, req.getParameter("comment"));
		if(Boolean.valueOf(req.getParameter("dontBotherMe")).booleanValue()){
			ScmSyncConfigurationDataProvider.provideBotherTimeout(req, req.getParameter("botherType"), 
					Integer.valueOf(req.getParameter("botherTime")), req.getParameter("currentURL"));
		}
	}
	
	// TODO: do retrieve help file with an action !
	public void doHelpForRepositoryUrl(StaplerRequest req, StaplerResponse res) throws ServletException, IOException{
    	req.getView(this, SCM.valueOf(req.getParameter("scm")).getRepositoryUrlHelpPath()).forward(req, res);
	}
	
	public void deleteHierarchy(File rootHierarchy){
		this.business.deleteHierarchy(createScmContext(), rootHierarchy, getCurrentUser());
	}
	
	public void renameHierarchy(File oldDir, File newDir){
		this.business.renameHierarchy(createScmContext(), oldDir, newDir, getCurrentUser());
	}
	
	public void synchronizeFile(File modifiedFile){
		this.business.synchronizeFile(createScmContext(), modifiedFile, getCurrentComment(), getCurrentUser());
	}
	
	private static String getCurrentComment(){
		StaplerRequest req = Stapler.getCurrentRequest();
		// Sometimes, request can be null : when hudson starts for example !
		String comment = null;
		if(req != null){
			comment = ScmSyncConfigurationDataProvider.retrieveComment(req, false);
		}
		return comment;
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
		ScmSyncConfigurationDataProvider.retrieveComment(Stapler.getCurrentRequest(), true);
		
		// Displaying commit message popup is based on following tests :
		// Zero : never ask for a commit message
		// First : no botherTimeout should match with current url
		// Second : a strategy should exist, matching current url
		// Third : SCM Sync should be settled up
		return !noUserCommitMessage && ScmSyncConfigurationDataProvider.retrieveBotherTimeoutMatchingUrl(Stapler.getCurrentRequest(), url) == null
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
	
	public boolean isDisplayStatus() {
		return displayStatus;
	}
	
    public String getCommitMessagePattern() {
        return commitMessagePattern;
    }

	public Descriptor<? extends hudson.scm.SCM> getDescriptorForSCM(String scmName){
		return SCM.valueOf(scmName).getSCMDescriptor();
	}
}
