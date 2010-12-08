package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(SCM.class)
public class InitRepositoryTest extends ScmSyncConfigurationBaseTest {

	private ScmSyncConfigurationBusiness sscBusiness;
	
	@Before
	public void initBusiness() throws Throwable{
		this.sscBusiness = new ScmSyncConfigurationBusiness();
		sscBusiness.start();
	}
	
	@After
	public void cleanBusiness() throws Throwable {
		sscBusiness.stop();
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
	
	protected String getSCMRepositoryURL(){
		return "scm:svn:file:///"+this.getCurentLocalSvnRepository().getAbsolutePath();
	}
}
