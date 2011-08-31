package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

public class DefaultSSCPOJO implements ScmSyncConfigurationPOJO {

	private String scmRepositoryUrl;
	private SCM scm;
	private String scmCommentPrefix;
	private String scmCommentSuffix;
	
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
	
	public String getScmCommentPrefix() {
		return scmCommentPrefix;
	}
	
	public void setScmCommentPrefix(String scmCommentPrefix) {
		this.scmCommentPrefix = scmCommentPrefix;
	}
	
	public String getScmCommentSuffix() {
		return scmCommentSuffix;
	}
	
	public void setScmCommentSuffix(String scmCommentSuffix) {
		this.scmCommentSuffix = scmCommentSuffix;
	}
}
