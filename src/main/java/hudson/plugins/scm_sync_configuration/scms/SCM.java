package hudson.plugins.scm_sync_configuration.scms;

import org.kohsuke.stapler.StaplerRequest;

public enum SCM {
	
	SUBVERSION("Subversion", "svn-config.jelly", "hudson.scm.SubversionSCM", "/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/SubversionSCM/scms/"){
		private static final String SCM_URL_PREFIX="scm:svn:";
		public String createScmUrlFromRequest(StaplerRequest req) {
			return SCM_URL_PREFIX+req.getParameter("repositoryUrl");
		}
		public String extractScmUrlFrom(String scmUrl) {
			return scmUrl.substring(SCM_URL_PREFIX.length());
		}
	};

	private String title;
	private String configPage;
	private String scmDescriptorClassName;
	private String repositoryUrlHelpPath;
	
	private SCM(String _title, String _configPage, String _scmDescriptorClassName, String _repositoryUrlHelpPath){
		this.title = _title;
		this.configPage = _configPage;
		this.scmDescriptorClassName = _scmDescriptorClassName;
		this.repositoryUrlHelpPath = _repositoryUrlHelpPath;
	}
	
	public String getTitle(){
		return this.title;
	}
	
	public String getConfigPage(){
		return this.configPage;
	}

	public String getSCMDescriptorClassName() {
		return this.scmDescriptorClassName;
	}
	public String getRepositoryUrlHelpPath() {
		return this.repositoryUrlHelpPath;
	}

	public abstract String createScmUrlFromRequest(StaplerRequest req);
	public abstract String extractScmUrlFrom(String scmUrl);
}
