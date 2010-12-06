package hudson.plugins.scm_sync_configuration.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import hudson.plugins.scm_sync_configuration.scms.SCM;

public class ScmContext {

	private String scmRepositoryUrl;
	private SCM scm;

	public ScmContext(SCM _scm, String _scmRepositoryUrl){
		this.scm = _scm;
		this.scmRepositoryUrl = _scmRepositoryUrl;
	}

	public String getScmRepositoryUrl() {
		return scmRepositoryUrl;
	}

	public SCM getScm() {
		return scm;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("scm", scm).append("scmRepositoryUrl", scmRepositoryUrl).toString();
	}
}
