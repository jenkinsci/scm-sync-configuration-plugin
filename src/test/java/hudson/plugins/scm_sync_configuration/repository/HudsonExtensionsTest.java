package hudson.plugins.scm_sync_configuration.repository;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationItemListener;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(ScmSyncConfigurationPlugin.class)
public class HudsonExtensionsTest extends ScmSyncConfigurationBaseTest {

	private ScmSyncConfigurationBusiness sscBusiness;
	private ScmSyncConfigurationItemListener sscItemListener;
	
	@Before
	public void initObjectsUnderTests() throws Throwable{
		this.sscBusiness = new ScmSyncConfigurationBusiness();
		this.sscItemListener = new ScmSyncConfigurationItemListener();

		// Mocking ScmSyncConfigurationPlugin singleton ...
		mockStatic(ScmSyncConfigurationPlugin.class);
		ScmSyncConfigurationPlugin mockedPlugin = createPartialMock(ScmSyncConfigurationPlugin.class, new String[]{ "getStrategyForSaveable" });
		mockedPlugin.setBusiness(this.sscBusiness);
		expect(ScmSyncConfigurationPlugin.getInstance()).andStubReturn(mockedPlugin);
		expect(mockedPlugin.getStrategyForSaveable(anyObject(Saveable.class), anyObject(File.class))).andStubReturn(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES[0]);
		
		replay(mockedPlugin);
		replay(ScmSyncConfigurationPlugin.class);
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
		Item mockedItem = createMock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		expect(mockedItem.getRootDir()).andStubReturn(mockedItemRootDir);
		replay(mockedItem);
		
		sscItemListener.onRenamed(mockedItem, "fakeJob", "newFakeJob");
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobRenameBeCorrectlyImpactedOnSCM/");
	}
	
	@Test
	public void shouldJobDeleteBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		// Deleting fakeJob
		Item mockedItem = createMock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		expect(mockedItem.getRootDir()).andStubReturn(mockedItemRootDir);
		replay(mockedItem);
		
		sscItemListener.onDeleted(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteBeCorrectlyImpactedOnSCM/");
	}
}
