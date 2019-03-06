package hudson.plugins.scm_sync_configuration.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.notNull;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncSubversionSCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;
import hudson.plugins.test.utils.PluginUtil;
import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;

import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(SaveableListener.class)
public class V0_0_3CompatibilityTest extends ScmSyncConfigurationPluginBaseTest {

	public V0_0_3CompatibilityTest() {
		super(new ScmUnderTestSubversion());
	}

	protected String getHudsonRootBaseTemplate() {
		return "hudsonRoot0.0.3BaseTemplate/";
	}
	
	@Test
	// JENKINS-8453 related
	public void should0_0_3_pluginConfigurationFileShouldLoadCorrectly() throws Throwable {
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		assertThat(plugin.getSCM(), is(notNullValue()));
		assertThat(plugin.getSCM().getId(), is(equalTo(ScmSyncSubversionSCM.class.getName())));
	}
	
	@Test
	public void should0_0_3_pluginConfigurationMigrationBeIdemPotent() throws Throwable {
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		
		// Plugin has been loaded : let's record scm & repository url
		String expectedRepositoryUrl = plugin.getScmRepositoryUrl();
		SCM expectedScm = plugin.getSCM();
		
		// Persisting data
		mockStatic(SaveableListener.class);
		doNothing().when(SaveableListener.class); SaveableListener.fireOnChange((Saveable)notNull(), (XmlFile)notNull());
		plugin.save();
		
		// Then reloading it...
		PluginUtil.loadPlugin(plugin);
		
		// Verifying repositoryUrl & SCM
		assertThat(plugin.getSCM().getId(), is(equalTo(expectedScm.getId())));
		assertThat(plugin.getScmRepositoryUrl(), is(equalTo(expectedRepositoryUrl)));
	}
}
