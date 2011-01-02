package hudson.plugins.scm_sync_configuration;

import hudson.Plugin;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.HudsonConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JobConfigScmSyncStrategy;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.acegisecurity.AccessDeniedException;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ScmSyncConfigurationPlugin extends Plugin{
	
	public static final transient ScmSyncStrategy[] AVAILABLE_STRATEGIES = new ScmSyncStrategy[]{
			new JobConfigScmSyncStrategy(),
			new HudsonConfigScmSyncStrategy()
	};
	
	transient private ScmSyncConfigurationBusiness business;
	private String scmRepositoryUrl;
	private SCM scm;

	public ScmSyncConfigurationPlugin(){
		setBusiness(new ScmSyncConfigurationBusiness());
	}
	
	@Override
	public void start() throws Exception {
		super.start();
		
		Hudson.XSTREAM.registerConverter(new SCM.SCMXStreamConverter());
		
		this.load();
		
		// SCMManagerFactory.start() must be called here instead of ScmSyncConfigurationItemListener.onLoaded()
		// because, for some unknown reasons, we reach plexus bootstraping exceptions when
		// calling Embedder.start() when everything is loaded (very strange...)
		SCMManagerFactory.getInstance().start();
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
			this.scm = SCM.valueOf(scmType);
			String newScmRepositoryUrl = this.scm.createScmUrlFromRequest(req);
			// If something changed, let's reinitialize repository in working directory !
			if(newScmRepositoryUrl != null && !newScmRepositoryUrl.equals(this.scmRepositoryUrl)){
				this.scmRepositoryUrl = newScmRepositoryUrl;
				this.save();
				
				this.business.initializeRepository(createScmContext(), true);
				this.business.synchronizeAllConfigs(createScmContext(), AVAILABLE_STRATEGIES, getCurrentUser());
			}
		}
	}
	
	public void doSubmitComment(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
		// TODO: complexify this in order to pass a strategy identifier in the session key
		ScmSyncConfigurationDataProvider.provideComment(req, req.getParameter("comment"));
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
		return new ScmContext(this.scm, this.scmRepositoryUrl);
	}
	
	public boolean shouldDecorationOccursOnURL(String url){
		// Removing comment from session here...
		ScmSyncConfigurationDataProvider.retrieveComment(Stapler.getCurrentRequest(), true);
		return getStrategyForURL(url) != null && this.business.scmConfigurationSettledUp(createScmContext());
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
	
	public SCM[] getScms(){
		return SCM.values();
	}

	public void setBusiness(ScmSyncConfigurationBusiness business) {
		this.business = business;
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
	
	public Descriptor<? extends hudson.scm.SCM> getDescriptorForSCM(String scmName){
		return SCM.valueOf(scmName).getSCMDescriptor();
	}
}
