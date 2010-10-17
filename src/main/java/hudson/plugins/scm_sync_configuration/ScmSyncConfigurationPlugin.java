package hudson.plugins.scm_sync_configuration;

import hudson.Plugin;
import hudson.XmlFile;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;
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
	
	private static final transient ScmSyncStrategy[] AVAILABLE_STRATEGIES = new ScmSyncStrategy[]{
			new JobConfigScmSyncStrategy(),
			new HudsonConfigScmSyncStrategy()
	};
	
	transient private ScmSyncConfigurationBusiness business;
	private String scmRepositoryUrl;
	private SCM scm;

	public ScmSyncConfigurationPlugin(){
		setBusiness(new ScmSyncConfigurationBusiness(this));
	}
	
	@Override
	public void start() throws Exception {
		super.start();
		this.load();
		this.business.start();
	}
	
	@Override
	public void stop() throws Exception {
		this.business.stop();
		super.stop();
	}
	
	@Override
	public void configure(StaplerRequest req, JSONObject formData)
			throws IOException, ServletException, FormException {
		super.configure(req, formData);
		
		this.scm = SCM.valueOf(req.getParameter("scm"));
		String newScmRepositoryUrl = this.scm.createScmUrlFromRequest(req);
		// If something changed, let's reinitialize repository in working directory !
		if(newScmRepositoryUrl != null && !newScmRepositoryUrl.equals(this.scmRepositoryUrl)){
			this.scmRepositoryUrl = newScmRepositoryUrl;
			this.save();
			
			this.business.initializeRepository(true);
			this.business.synchronizeAllConfigs(AVAILABLE_STRATEGIES, getCurrentUser());
		}
	}
	
	public void doSubmitComment(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
		// TODO: complexify this in order to pass a strategy identifier in the session key
		req.getSession().setAttribute("commitMessage", req.getParameter("comment"));
	}
	
	// TODO: do retrieve help file with an action !
	public void doHelpForRepositoryUrl(StaplerRequest req, StaplerResponse res) throws ServletException, IOException{
    	req.getView(this, SCM.valueOf(req.getParameter("scm")).getRepositoryUrlHelpPath()).forward(req, res);
	}
	
	public void commitFile(XmlFile modifiedFile){
		this.business.synchronizeFile(modifiedFile.getFile(), getCurrentComment(), getCurrentUser());
	}
	
	private static String getCurrentComment(){
		StaplerRequest req = Stapler.getCurrentRequest();
		// Sometimes, request can be null : when hudson starts for example !
		String comment = null;
		if(req != null){
			comment = (String)req.getSession().getAttribute("commitMessage");
		}
		return comment;
	}
	
	private static User getCurrentUser(){
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
	
	public boolean shouldDecorationOccursOnURL(String url){
		return getStrategyForURL(url) != null && this.business.scmConfigurationSettledUp();
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
	
	public String getScmUrl(){
		if(this.scm != null){
			return this.scm.extractScmUrlFrom(this.scmRepositoryUrl);
		} else {
			return null;
		}
	}
	
	public Descriptor getDescriptorForSCM(String scmName){
		return Hudson.getInstance().getDescriptorByName(SCM.valueOf(scmName).getSCMDescriptorClassName());
	}
}
