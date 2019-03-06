package hudson.plugins.scm_sync_configuration.model;

import org.apache.commons.lang.builder.ToStringBuilder;

import hudson.plugins.scm_sync_configuration.scms.SCM;

public class ScmContext {

	private String scmRepositoryUrl;
	private String gitRepositoryBranch;
	private SCM scm;
    private String commitMessagePattern;

    public ScmContext(SCM _scm, String _scmRepositoryUrl){
        this(_scm, _scmRepositoryUrl, "[message]", "master");
    }

	public ScmContext(SCM _scm, String _scmRepositoryUrl, String _commitMessagePattern, String gitRepositoryBranch){
		this.scm = _scm;
		this.scmRepositoryUrl = _scmRepositoryUrl;
        this.commitMessagePattern = _commitMessagePattern;
        this.gitRepositoryBranch = gitRepositoryBranch;
	}

	public String getGitRepositoryBranch() {
		return gitRepositoryBranch;
	}

	public String getScmRepositoryUrl() {
		return scmRepositoryUrl;
	}

	public SCM getScm() {
		return scm;
	}

    public String getCommitMessagePattern(){
        return commitMessagePattern;
    }
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("scm", scm).append("scmRepositoryUrl", scmRepositoryUrl)
                .append("commitMessagePattern", commitMessagePattern)
				.append("gitRepositoryBranch", gitRepositoryBranch).toString();
	}
}
