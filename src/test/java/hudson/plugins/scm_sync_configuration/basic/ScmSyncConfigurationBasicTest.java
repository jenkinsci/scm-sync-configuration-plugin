package hudson.plugins.scm_sync_configuration.basic;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import jenkins.model.Jenkins;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;

import java.io.File;

import org.junit.Test;

public class ScmSyncConfigurationBasicTest extends ScmSyncConfigurationBaseTest {

	public ScmSyncConfigurationBasicTest() {
		super(new ScmUnderTestSubversion());
	}
	
	@Test
	public void shouldRetrieveMockedHudsonInstanceCorrectly() throws Throwable {
		Jenkins jenkinsInstance = Jenkins.getInstance();
		assertThat(jenkinsInstance, is(notNullValue()));
		assertThat(jenkinsInstance.toString().split("@")[0], is(not(equalTo("jenkins.model.Jenkins"))));
	}
	
	@Test
	public void shouldVerifyIfHudsonRootDirectoryExists() throws Throwable {
		
		Jenkins jenkinsInstance = Jenkins.getInstance();
		File hudsonRootDir = jenkinsInstance.getRootDir();
		assertThat(hudsonRootDir, is(not(equalTo(null))));
		assertThat(hudsonRootDir.exists(), is(true));
	}
}
