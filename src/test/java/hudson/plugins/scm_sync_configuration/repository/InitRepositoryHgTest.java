package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.test.utils.scms.ScmUnderTestHg;



public class InitRepositoryHgTest extends InitRepositoryTest {

	public InitRepositoryHgTest() {
		super(new ScmUnderTestHg());
	}

}
