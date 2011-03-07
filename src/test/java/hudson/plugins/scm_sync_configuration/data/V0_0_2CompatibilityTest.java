package hudson.plugins.scm_sync_configuration.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncSubversionSCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;

import org.junit.Test;

public class V0_0_2CompatibilityTest extends ScmSyncConfigurationPluginBaseTest {

	protected String getHudsonRootBaseTemplate() {
		if("should0_0_2_pluginConfigurationFileShouldLoadCorrectly".equals(testName.getMethodName())){
			return "hudsonRoot0.0.2BaseTemplate/";
		} else if("should0_0_2_pluginEmptyConfigurationFileShouldLoadCorrectly".equals(testName.getMethodName())){
			return "hudsonRoot0.0.2WithEmptyConfTemplate/";
		} else {
			throw new IllegalArgumentException("Unsupported test name : "+testName);
		}
	}
	
	@Test
	// JENKINS-8453 related
	public void should0_0_2_pluginConfigurationFileShouldLoadCorrectly() throws Throwable {
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		assertThat(plugin.getSCM(), is(notNullValue()));
		assertThat(plugin.getSCM().getId(), is(equalTo(ScmSyncSubversionSCM.class.getName())));
	}
	
	@Test
	public void should0_0_2_pluginEmptyConfigurationFileShouldLoadCorrectly() throws Throwable {
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		assertThat(plugin.getSCM(), is(notNullValue()));
		assertThat(plugin.getSCM().getId(), is(equalTo(ScmSyncNoSCM.class.getName())));
	}
}
