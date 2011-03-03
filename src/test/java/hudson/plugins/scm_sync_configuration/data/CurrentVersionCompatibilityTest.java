package hudson.plugins.scm_sync_configuration.data;

import static org.easymock.EasyMock.isNull;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncSubversionSCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;

import org.junit.Test;

public class CurrentVersionCompatibilityTest extends ScmSyncConfigurationBaseTest {

	protected String getHudsonRootBaseTemplate() {
		// Use default template directory...
		return super.getHudsonRootBaseTemplate();
	}
	
	@Test
	public void shouldCurrentVersionpluginConfigurationFileLoadCorrectly() throws Throwable {
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		assertThat(plugin.getSCM(), not(isNull()));
		assertThat(plugin.getSCM().getId(), is(equalTo(ScmSyncSubversionSCM.class.getName())));
	}
}
