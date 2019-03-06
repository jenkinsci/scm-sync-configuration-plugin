package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;

public class InitRepositorySubversionTest extends InitRepositoryTest {

	public InitRepositorySubversionTest() {
		super(new ScmUnderTestSubversion());
	}

}
