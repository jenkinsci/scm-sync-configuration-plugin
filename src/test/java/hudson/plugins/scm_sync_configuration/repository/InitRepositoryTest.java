package hudson.plugins.scm_sync_configuration.repository;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;

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
		sscBusiness.scmConfigurationSettledUp(scmContext);
		assert sscBusiness.scmConfigurationSettledUp(scmContext);
	}
	
	protected String getSCMRepositoryURL(){
		return "scm:svn:file:///"+this.getCurentLocalSvnRepository().getAbsolutePath();
	}
}
