package hudson.plugins.scm_sync_configuration.repository;

import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.SCMManagerFactory;
import hudson.plugins.scm_sync_configuration.SCMManipulator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;
import hudson.plugins.test.utils.DirectoryUtils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.core.io.ClassPathResource;

@PrepareForTest(SCM.class)
public class InitRepositoryTest extends ScmSyncConfigurationBaseTest {

	private ScmSyncConfigurationBusiness sscBusiness;
	
	@Before
	public void initBusiness() throws Throwable{
		this.sscBusiness = new ScmSyncConfigurationBusiness();
	}
	
	@Test
	public void shouldNotInitializeAnyRepositoryWhenScmContextIsEmpty() throws Throwable {
		ScmContext emptyContext = new ScmContext(null, null);
		sscBusiness.init(emptyContext);
		assert !sscBusiness.scmConfigurationSettledUp(emptyContext);
		
		emptyContext = new ScmContext(null, getSCMRepositoryURL());
		sscBusiness.init(emptyContext);
		assert !sscBusiness.scmConfigurationSettledUp(emptyContext);
		
		SCM mockedSCM = createSCMMock(true);
		emptyContext = new ScmContext(mockedSCM, null);
		sscBusiness.init(emptyContext);
		assert !sscBusiness.scmConfigurationSettledUp(emptyContext);
	}
	
	@Test
	public void shouldInitializeLocalRepositoryWhenScmContextIsCorrect() throws Throwable {
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		assert sscBusiness.scmConfigurationSettledUp(scmContext);
	}
	
	@Test
	public void shouldInitializeLocalRepositoryWhenScmContextIsCorrentAndEvenIfScmDirectoryDoesntExist() throws Throwable {
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		assert sscBusiness.scmConfigurationSettledUp(scmContext);
	}
	
	@Override
	public File getCurrentScmSyncConfigurationCheckoutDirectory() {
		File scmSyncConfigurationCheckoutRootDir = super.getCurrentScmSyncConfigurationCheckoutDirectory();
		if("shouldInitializeLocalRepositoryWhenScmContextIsCorrentAndEvenIfScmDirectoryDoesntExist".equals(testName.getMethodName())){
			return new File(scmSyncConfigurationCheckoutRootDir.getAbsolutePath()+"/path/that/doesnt/exist/");
		} else {
			return scmSyncConfigurationCheckoutRootDir;
		}
	}
	
	@Test
	public void shouldResetCheckoutConfigurationDirectoryWhenAsked() throws Throwable {
		// Initializing the repository...
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		
		// After init, local checkouted repository should exists
		assert getCurrentScmSyncConfigurationCheckoutDirectory().exists();
		
		// Populating checkoutConfiguration directory ..
		File fileWhichShouldBeDeletedAfterReset = new File(getCurrentScmSyncConfigurationCheckoutDirectory().getAbsolutePath()+"/hello.txt");
		assert fileWhichShouldBeDeletedAfterReset.createNewFile();
		FileUtils.fileWrite(fileWhichShouldBeDeletedAfterReset.getAbsolutePath(), "Hello world !");
		
		// Reseting the repository, without cleanup
		sscBusiness.initializeRepository(scmContext, false);
		assert fileWhichShouldBeDeletedAfterReset.exists();
		
		// Reseting the repository with cleanup
		sscBusiness.initializeRepository(scmContext, true);
		assert !fileWhichShouldBeDeletedAfterReset.exists();
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
