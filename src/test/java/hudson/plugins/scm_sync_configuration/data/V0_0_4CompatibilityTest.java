package hudson.plugins.scm_sync_configuration.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;

import org.junit.Test;

public class V0_0_4CompatibilityTest extends ScmSyncConfigurationPluginBaseTest {

	public V0_0_4CompatibilityTest() {
		super(new ScmUnderTestSubversion());
	}

	protected String getHudsonRootBaseTemplate() {
		return "hudsonRoot0.0.4WithEmptyConfTemplate/";
	}
	
	@Test
	public void should0_0_4_pluginEmptyConfigurationFileShouldLoadCorrectly() throws Throwable {
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		assertThat(plugin.getSCM(), is(notNullValue()));
		assertThat(plugin.getSCM().getId(), is(equalTo(ScmSyncNoSCM.class.getName())));
	}
}
