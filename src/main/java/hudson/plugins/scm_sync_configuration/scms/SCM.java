package hudson.plugins.scm_sync_configuration.scms;

import org.kohsuke.stapler.StaplerRequest;

public enum SCM {
	
	SUBVERSION("Subversion", "svn-config.jelly"){
		public String createScmUrlFromRequest(StaplerRequest req) {
			return "scm:svn:"+req.getParameter("repositoryUrl");
		}
	};

	private String title;
	private String configPage;
	
	private SCM(String _title, String _configPage){
		this.title = _title;
		this.configPage = _configPage;
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public String getConfigPage(){
		return this.configPage;
	}
	
	public abstract String createScmUrlFromRequest(StaplerRequest req);
}
