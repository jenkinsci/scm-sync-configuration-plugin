package hudson.plugins.scm_sync_configuration;

import hudson.Plugin;
import hudson.XmlFile;
import hudson.model.Descriptor.FormException;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JobConfigScmSyncStrategy;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class ScmSyncConfigurationPlugin extends Plugin{
	
	private static final transient ScmSyncStrategy[] AVAILABLE_STRATEGIES = new ScmSyncStrategy[]{
			new JobConfigScmSyncStrategy()
	};
	
	transient private ScmSyncConfigurationBusiness business;
	private String scmRepositoryUrl;

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
		
		this.scmRepositoryUrl = createScmRepositoryUrl(req);
		this.save();
		
		this.business.initializeRepository();
	}
	
	public static String createScmRepositoryUrl(StaplerRequest req){
		SCM scm = retrieveSCMFromRequest(req);
		return scm.createScmUrlFromRequest(req);
	}
	
	private static SCM retrieveSCMFromRequest(StaplerRequest req){
		return SCM.valueOf(req.getParameter("scm"));
	}
	
	public void doSubmitComment(StaplerRequest req, StaplerResponse res) throws ServletException, IOException {
		// TODO: complexify this in order to pass a strategy identifier in the session key
		req.getSession().setAttribute("commitMessage", req.getParameter("comment"));
	}
	
	public void commitFile(XmlFile modifiedFile, String comment, User user){
		this.business.synchronizeFile(modifiedFile.getFile(), comment, user);
	}
	
	public static ScmSyncConfigurationPlugin getInstance(){
		return Hudson.getInstance().getPlugin(ScmSyncConfigurationPlugin.class);
	}
	
	public ScmSyncStrategy getStrategyForSaveable(Saveable s){
		for(ScmSyncStrategy strat : AVAILABLE_STRATEGIES){
			if(strat.isSaveableApplicable(s)){
				return strat;
			}
		}
		// Strategy not found !
		return null;
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
}
