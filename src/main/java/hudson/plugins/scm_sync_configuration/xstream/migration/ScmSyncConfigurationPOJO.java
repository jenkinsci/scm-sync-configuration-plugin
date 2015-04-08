package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.util.List;

/**
 * Generic interface for ScmSyncConfiguration POJOs
 * @author fcamblor
 */
public interface ScmSyncConfigurationPOJO {
	public String getScmRepositoryUrl();
	public void setScmRepositoryUrl(String scmRepositoryUrl);
    public String getScmGitBranch();
    public void setScmGitBranch(String scmGitBranch);
	public SCM getScm();
	public void setScm(SCM scm);
	public boolean isNoUserCommitMessage();
	public void setNoUserCommitMessage(boolean noUserCommitMessage);
	public boolean isDisplayStatus();
	public void setDisplayStatus(boolean displayStatus);
    public String getCommitMessagePattern();
    public void setCommitMessagePattern(String commitMessagePattern);
    public List<String> getManualSynchronizationIncludes();
    public void setManualSynchronizationIncludes(List<String> manualSynchronizationIncludes);
}
