package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

public class DefaultSSCPOJO implements ScmSyncConfigurationPOJO {

	private String scmRepositoryUrl;
	private SCM scm;
	private boolean noUserCommitMessage;
	private boolean displayStatus;
    private String commitMessagePattern;
	
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
	public boolean isNoUserCommitMessage() {
		return noUserCommitMessage;
	}
	public void setNoUserCommitMessage(boolean noUserCommitMessage) {
		this.noUserCommitMessage = noUserCommitMessage;
	}
	public boolean isDisplayStatus() {
		return displayStatus;
	}
	public void setDisplayStatus(boolean displayStatus) {
		this.displayStatus = displayStatus;
	}

    public String getCommitMessagePattern() {
        return commitMessagePattern;
    }

    public void setCommitMessagePattern(String commitMessagePattern) {
        this.commitMessagePattern = commitMessagePattern;
    }
}
