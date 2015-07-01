package hudson.plugins.scm_sync_configuration.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import hudson.plugins.scm_sync_configuration.scms.SCM;

public class ScmContext {

	private String scmRepositoryUrl;
	private SCM scm;
    private String commitMessagePattern;
    private String scmGitBranch;

    public ScmContext(SCM _scm, String _scmRepositoryUrl){
        this(_scm, _scmRepositoryUrl, "[message]");
    }

	public ScmContext(SCM _scm, String _scmRepositoryUrl, String _commitMessagePattern){
		this(_scm, _scmRepositoryUrl, _commitMessagePattern, null);
	}

    public ScmContext(SCM _scm, String _scmRepositoryUrl, String _commitMessagePattern, String _scmGitBranch){
        this.scm = _scm;
        this.scmRepositoryUrl = _scmRepositoryUrl;
        this.commitMessagePattern = _commitMessagePattern;
        this.scmGitBranch = _scmGitBranch;
    }

	public String getScmRepositoryUrl() {
		return scmRepositoryUrl;
	}

	public SCM getScm() {
		return scm;
	}

    public String getScmGitBranch() {
        return scmGitBranch;
    }

    public String getCommitMessagePattern(){
        return commitMessagePattern;
    }
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("scm", scm).append("scmRepositoryUrl", scmRepositoryUrl)
                .append("commitMessagePattern", commitMessagePattern).toString();
	}
}
