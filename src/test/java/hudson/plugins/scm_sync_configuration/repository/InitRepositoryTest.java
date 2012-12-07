package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@PrepareForTest(SCM.class)
public abstract class InitRepositoryTest extends ScmSyncConfigurationPluginBaseTest {
	
	protected InitRepositoryTest(ScmUnderTest scmUnderTest) {
		super(scmUnderTest);
	}
	
	@Test
	public void shouldNotInitializeAnyRepositoryWhenScmContextIsEmpty() throws Throwable {
		ScmContext emptyContext = new ScmContext(null, null);
		sscBusiness.init(emptyContext);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(emptyContext), is(false));
		
		emptyContext = new ScmContext(null, getSCMRepositoryURL());
		sscBusiness.init(emptyContext);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(emptyContext), is(false));
		
		createSCMMock(null);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(emptyContext), is(false));
		
		assertStatusManagerIsNull();
	}
	
	@Test
	@Ignore("Not yet implemented ! (it is difficult because svn list/log has not yet been implemented in svnjava impl")
	public void shouldInitializeLocalRepositoryWhenScmContextIsCorrentAndEvenIfScmDirectoryDoesntExist() throws Throwable {
		createSCMMock();
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(scmContext), is(true));
	}
	
	@Test
	public void shouldResetCheckoutConfigurationDirectoryWhenAsked() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// After init, local checkouted repository should exists
		assertThat(getCurrentScmSyncConfigurationCheckoutDirectory().exists(), is(true));
		
		// Populating checkoutConfiguration directory ..
		File fileWhichShouldBeDeletedAfterReset = new File(getCurrentScmSyncConfigurationCheckoutDirectory().getAbsolutePath()+"/hello.txt");
		assertThat(fileWhichShouldBeDeletedAfterReset.createNewFile(), is(true));
		FileUtils.fileWrite(fileWhichShouldBeDeletedAfterReset.getAbsolutePath(), "Hello world !");
		
		// Reseting the repository, without cleanup
		sscBusiness.initializeRepository(scmContext, false);
		if (this instanceof InitRepositoryHgTest) {
			//HG behaves differently and always destroys files that aren't in source control
			//Also should this be allowed in the first place. If a reset is requested why should it
			//allow untracked files to linger around????
			assertThat(fileWhichShouldBeDeletedAfterReset.exists(), is(false));
		} else {
			assertThat(fileWhichShouldBeDeletedAfterReset.exists(), is(true));
		}
		
		// Reseting the repository with cleanup
		sscBusiness.initializeRepository(scmContext, true);
		assertThat(fileWhichShouldBeDeletedAfterReset.exists(), is(false));
		
		assertStatusManagerIsOk();
	}
	
	@Test
	public void shouldSynchronizeHudsonFiles() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/InitRepositoryTest.shouldSynchronizeHudsonFiles/");
		
		assertStatusManagerIsOk();
	}
	
	@Test
	public void shouldInitializeLocalRepositoryWhenScmContextIsCorrect()
			throws Throwable {
		createSCMMock();
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(scmContext), is(true));
		
		assertStatusManagerIsOk();
	}

}
