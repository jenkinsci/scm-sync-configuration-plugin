package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.test.utils.scms.ScmUnderTestGit;
import org.junit.Ignore;

public class HudsonExtensionsGitTest extends HudsonExtensionsTest {

	public HudsonExtensionsGitTest() {
		super(new ScmUnderTestGit());
	}
}
