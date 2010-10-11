package hudson.plugins.scm_sync_configuration.scms;

import org.kohsuke.stapler.StaplerRequest;

public enum SCM {
	
	SUBVERSION("Subversion", "svn-config.jelly"){
		private static final String SCM_URL_PREFIX="scm:svn:";
		public String createScmUrlFromRequest(StaplerRequest req) {
			return SCM_URL_PREFIX+req.getParameter("repositoryUrl");
		}
		public String extractScmUrlFrom(String scmUrl) {
			return scmUrl.substring(SCM_URL_PREFIX.length());
		}
		public String getSCMDescriptorClassName() {
			return "hudson.scm.SubversionSCM";
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
	public abstract String extractScmUrlFrom(String scmUrl);
	public abstract String getSCMDescriptorClassName();
}
