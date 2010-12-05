package hudson.plugins.scm_sync_configuration.basic;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;

import org.junit.Test;

public class ScmSyncConfigurationBasicTest extends ScmSyncConfigurationBaseTest {

	@Test
	public void shouldRetrieveMockedHudsonInstanceCorrectly() throws Throwable {
		
		Hudson hudsonInstance = Hudson.getInstance();
		assertThat(hudsonInstance, is(notNullValue()));
		assertThat(hudsonInstance.toString().split("@")[0], is(not(equalTo("hudson.model.Hudson"))));
	}
}
