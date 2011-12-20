package hudson.plugins.scm_sync_configuration.util;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.test.utils.scms.ScmUnderTest;

// Class will start current ScmSyncConfigurationPlugin instance
public class ScmSyncConfigurationPluginBaseTest extends
		ScmSyncConfigurationBaseTest {

	protected ScmSyncConfigurationPluginBaseTest(ScmUnderTest scmUnderTest) {
		super(scmUnderTest);
	}

	public void setup() throws Throwable {
		super.setup();

		// Let's start the plugin...
		ScmSyncConfigurationPlugin.getInstance().start();
	}

	public void teardown() throws Throwable {
		// Stopping current plugin
		ScmSyncConfigurationPlugin.getInstance().stop();

		super.teardown();
	}
	
	protected void assertStatusManagerIsOk() {
		assertThat(sscBusiness.getScmSyncConfigurationStatusManager().getLastFail(), nullValue());
		assertThat(sscBusiness.getScmSyncConfigurationStatusManager().getLastSuccess(), notNullValue());
	}
		    
	protected void assertStatusManagerIsNull() {
		assertThat(sscBusiness.getScmSyncConfigurationStatusManager().getLastFail(), nullValue());
		assertThat(sscBusiness.getScmSyncConfigurationStatusManager().getLastSuccess(), nullValue());
	}

}
