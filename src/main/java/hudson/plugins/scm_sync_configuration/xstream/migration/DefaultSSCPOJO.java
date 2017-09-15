package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.util.List;

public class DefaultSSCPOJO implements ScmSyncConfigurationPOJO {

	private String scmRepositoryUrl;
	private SCM scm;
	private boolean noUserCommitMessage;
	private boolean displayStatus;
	private boolean syncJenkinsConfig;
	private boolean syncBasicPluginsConfig;
	private boolean syncJobConfig;
	private boolean syncUserConfig;


    private String commitMessagePattern;
    private List<String> manualSynchronizationIncludes;
	
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
	
	public boolean isSyncJenkinsConfig() {
		return syncJenkinsConfig;
	}
	public void setSyncJenkinsConfig(boolean syncJenkinsConfig) {
		this.syncJenkinsConfig = syncJenkinsConfig;
	}
	public boolean isSyncBasicPluginsConfig() {
		return syncBasicPluginsConfig;
	}
	public void setSyncBasicPluginsConfig(boolean syncBasicPluginsConfig) {
		this.syncBasicPluginsConfig = syncBasicPluginsConfig;
	}
	public boolean isSyncJobConfig() {
		return syncJobConfig;
	}
	public void setSyncJobConfig(boolean syncJobConfig) {
		this.syncJobConfig = syncJobConfig;
	}
	public boolean isSyncUserConfig() {
		return syncUserConfig;
	}
	public void setSyncUserConfig(boolean syncUserConfig) {
		this.syncUserConfig = syncUserConfig;
	}

    public String getCommitMessagePattern() {
        return commitMessagePattern;
    }

    public void setCommitMessagePattern(String commitMessagePattern) {
        this.commitMessagePattern = commitMessagePattern;
    }

    public void setManualSynchronizationIncludes(List<String> _manualSynchronizationIncludes){
        this.manualSynchronizationIncludes = _manualSynchronizationIncludes;
    }

    public List<String> getManualSynchronizationIncludes(){
        return this.manualSynchronizationIncludes;
    }
}
