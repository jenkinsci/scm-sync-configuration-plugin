package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

public class DefaultSSCPOJO implements ScmSyncConfigurationPOJO {

	private String scmRepositoryUrl;
	private SCM scm;
	private boolean displayStatus;
	
	public String getScmRepositoryUrl() {
		return scmRepositoryUrl;
	}
	public void setScmRepositoryUrl(String scmRepositoryUrl) {
		this.scmRepositoryUrl = scmRepositoryUrl;
	}
	public SCM getScm() {
		return scm;
	}
	public void setScm(SCM scm) {
		this.scm = scm;
	}
	public boolean isDisplayStatus() {
		return displayStatus;
	}
	public void setDisplayStatus(boolean displayStatus) {
		this.displayStatus = displayStatus;
	}
}
