package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;


public class HudsonExtensionsSubversionTest extends HudsonExtensionsTest {

	public HudsonExtensionsSubversionTest() {
		super(new ScmUnderTestSubversion());
	}

}
