package hudson.plugins.scm_sync_configuration.repository;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;

public class HudsonExtensionsTest extends ScmSyncConfigurationBaseTest {

	private ScmSyncConfigurationBusiness sscBusiness;
	
	@Before
	public void initBusiness() throws Throwable{
		this.sscBusiness = new ScmSyncConfigurationBusiness();
	}
	
	@Test
	public void shouldJobRenameBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		// Renaming fakeJob to newFakeJob
		File oldDir = new File(getCurrentHudsonRootDirectory() + File.separator + "jobs/fakeJob/" );
		File newDir = new File(getCurrentHudsonRootDirectory() + File.separator + "jobs/newFakeJob/" );
		sscBusiness.renameHierarchy(scmContext, oldDir, newDir, Hudson.getInstance().getMe());
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobRenameBeCorrectlyImpactedOnSCM/");
	}
	
}
