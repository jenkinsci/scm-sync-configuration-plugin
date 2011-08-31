package hudson.plugins.scm_sync_configuration.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import hudson.plugins.scm_sync_configuration.scms.SCM;

public class ScmContext {

	private String scmRepositoryUrl;
	private String scmCommentPrefix;
	private String scmCommentSuffix;
	private SCM scm;

	public ScmContext(SCM _scm, String _scmRepositoryUrl, String _scmCommentPrefix, String _scmCommentSuffix) {
		this.scm = _scm;
		this.scmRepositoryUrl = _scmRepositoryUrl;
		this.scmCommentPrefix = _scmCommentPrefix;
		this.scmCommentSuffix = _scmCommentSuffix;
	}

	public String getScmRepositoryUrl() {
		return scmRepositoryUrl;
	}

	public String getScmCommentPrefix() {
		return scmCommentPrefix;
	}
	
	public String getScmCommentSuffix() {
		return scmCommentSuffix;
	}
	
	public SCM getScm() {
		return scm;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("scm", scm).append("scmRepositoryUrl", scmRepositoryUrl).toString();
	}
}
