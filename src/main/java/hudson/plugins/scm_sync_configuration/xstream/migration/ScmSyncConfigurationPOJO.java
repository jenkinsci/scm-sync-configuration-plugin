package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

/**
 * Generic interface for ScmSyncConfiguration POJOs
 * @author fcamblor
 */
public interface ScmSyncConfigurationPOJO {
	public String getScmRepositoryUrl();
	public void setScmRepositoryUrl(String scmRepositoryUrl);
	public SCM getScm();
	public void setScm(SCM scm);
	public boolean isNoUserCommitMessage();
	public void setNoUserCommitMessage(boolean noUserCommitMessage);
	public boolean isDisplayStatus();
	public void setDisplayStatus(boolean displayStatus);
    public String getCommitMessagePattern();
    public void setCommitMessagePattern(String commitMessagePattern);
}
