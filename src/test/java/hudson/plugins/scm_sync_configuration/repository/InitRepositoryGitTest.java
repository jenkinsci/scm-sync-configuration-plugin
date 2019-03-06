package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.test.utils.scms.ScmUnderTestGit;



public class InitRepositoryGitTest extends InitRepositoryTest {

	public InitRepositoryGitTest() {
		super(new ScmUnderTestGit());
	}

}
