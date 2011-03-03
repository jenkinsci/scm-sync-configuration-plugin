package hudson.plugins.scm_sync_configuration.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(SCM.class)
public class InitRepositoryTest extends ScmSyncConfigurationPluginBaseTest {

	private ScmSyncConfigurationBusiness sscBusiness;
	
	@Before
	public void initBusiness() throws Throwable{
		this.sscBusiness = new ScmSyncConfigurationBusiness();
	}
	
	@Test
	public void shouldNotInitializeAnyRepositoryWhenScmContextIsEmpty() throws Throwable {
		ScmContext emptyContext = new ScmContext(null, null);
		sscBusiness.init(emptyContext);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(emptyContext), is(false));
		
		emptyContext = new ScmContext(null, getSCMRepositoryURL());
		sscBusiness.init(emptyContext);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(emptyContext), is(false));
		
		SCM mockedSCM = createSCMMock(true);
		emptyContext = new ScmContext(mockedSCM, null);
		sscBusiness.init(emptyContext);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(emptyContext), is(false));
	}
	
	@Test
	public void shouldInitializeLocalRepositoryWhenScmContextIsCorrect() throws Throwable {
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(scmContext), is(true));
	}
	
	@Test
	@Ignore("Not yet implemented ! (it is difficult because svn list/log has not yet been implemented in svnjava impl")
	public void shouldInitializeLocalRepositoryWhenScmContextIsCorrentAndEvenIfScmDirectoryDoesntExist() throws Throwable {
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL()+"/path/that/doesnt/exist/");
		sscBusiness.init(scmContext);
		assertThat(sscBusiness.scmCheckoutDirectorySettledUp(scmContext), is(true));
	}
	
	@Test
	public void shouldResetCheckoutConfigurationDirectoryWhenAsked() throws Throwable {
		// Initializing the repository...
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		
		// After init, local checkouted repository should exists
		assertThat(getCurrentScmSyncConfigurationCheckoutDirectory().exists(), is(true));
		
		// Populating checkoutConfiguration directory ..
		File fileWhichShouldBeDeletedAfterReset = new File(getCurrentScmSyncConfigurationCheckoutDirectory().getAbsolutePath()+"/hello.txt");
		assertThat(fileWhichShouldBeDeletedAfterReset.createNewFile(), is(true));
		FileUtils.fileWrite(fileWhichShouldBeDeletedAfterReset.getAbsolutePath(), "Hello world !");
		
		// Reseting the repository, without cleanup
		sscBusiness.initializeRepository(scmContext, false);
		assertThat(fileWhichShouldBeDeletedAfterReset.exists(), is(true));
		
		// Reseting the repository with cleanup
		sscBusiness.initializeRepository(scmContext, true);
		assertThat(fileWhichShouldBeDeletedAfterReset.exists(), is(false));
	}
	
	@Test
	public void shouldSynchronizeHudsonFiles() throws Throwable {
		// Initializing the repository...
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/InitRepositoryTest.shouldSynchronizeHudsonFiles/");
	}
}
