package hudson.plugins.scm_sync_configuration.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncSubversionSCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;

import org.junit.Test;

public class CurrentVersionCompatibilityTest extends ScmSyncConfigurationPluginBaseTest {

	protected String getHudsonRootBaseTemplate() {
		// Use default template directory...
		return super.getHudsonRootBaseTemplate();
	}
	
	@Test
	public void shouldCurrentVersionpluginConfigurationFileLoadCorrectly() throws Throwable {
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		assertThat(plugin.getSCM(), is(notNullValue()));
		assertThat(plugin.getSCM().getId(), is(equalTo(ScmSyncSubversionSCM.class.getName())));
	}
}
