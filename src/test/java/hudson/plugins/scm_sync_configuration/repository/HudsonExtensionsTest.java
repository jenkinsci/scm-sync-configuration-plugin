package hudson.plugins.scm_sync_configuration.repository;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.SCMManipulator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationItemListener;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;

import java.io.File;
import java.util.ArrayList;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(ScmSyncConfigurationPlugin.class)
public class HudsonExtensionsTest extends ScmSyncConfigurationPluginBaseTest {

	private ScmSyncConfigurationBusiness sscBusiness;
	private ScmSyncConfigurationItemListener sscItemListener;
	
	@Before
	public void initObjectsUnderTests() throws Throwable{
		this.sscBusiness = new ScmSyncConfigurationBusiness();
		this.sscItemListener = new ScmSyncConfigurationItemListener();

		// Mocking ScmSyncConfigurationPlugin.getStrategyForSaveable()
		ScmSyncConfigurationPlugin sscPlugin = spy(ScmSyncConfigurationPlugin.getInstance());
		sscPlugin.setBusiness(this.sscBusiness);
		PowerMockito.doReturn(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES[0]).when(sscPlugin).getStrategyForSaveable(Mockito.any(Saveable.class), Mockito.any(File.class));
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
		Item mockedItem = Mockito.mock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onRenamed(mockedItem, "fakeJob", "newFakeJob");
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobRenameBeCorrectlyImpactedOnSCM/");
	}
	
	@Test
	public void shouldJobRenameDoesntPerformAnyScmUpdate() throws Throwable {
		// Initializing the repository...
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		// Let's checkout current scm view ... and commit something in it ...
		SCMManipulator scmManipulator = createMockedScmManipulator();
		File checkoutDirectoryForVerifications = createTmpDirectory(this.getClass().getSimpleName()+"_"+testName.getMethodName()+"__tmpHierarchyForCommit");
		scmManipulator.checkout(checkoutDirectoryForVerifications);
		final File hello1 = new File(checkoutDirectoryForVerifications.getAbsolutePath()+"/jobs/hello.txt");
		final File hello2 = new File(checkoutDirectoryForVerifications.getAbsolutePath()+"/hello2.txt");
		FileUtils.fileAppend(hello1.getAbsolutePath(), "hello world !");
		FileUtils.fileAppend(hello2.getAbsolutePath(), "hello world 2 !");
		scmManipulator.addFile(checkoutDirectoryForVerifications, "jobs/hello.txt");
		scmManipulator.addFile(checkoutDirectoryForVerifications, "hello2.txt");
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, new ArrayList<File>(){{ add(hello1); add(hello2); }}, "external commit");

		
		// Renaming fakeJob to newFakeJob
		Item mockedItem = Mockito.mock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onRenamed(mockedItem, "fakeJob", "newFakeJob");
		
		// Assert no hello file is present in current hudson root
		assert !new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists();
		assert !new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists();
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
		Item mockedItem = Mockito.mock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onDeleted(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteBeCorrectlyImpactedOnSCM/");
	}

	@Test
	public void shouldJobDeleteDoesntPerformAnyScmUpdate() throws Throwable {
		// Initializing the repository...
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		sscBusiness.init(scmContext);
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		// Let's checkout current scm view ... and commit something in it ...
		SCMManipulator scmManipulator = createMockedScmManipulator();
		File checkoutDirectoryForVerifications = createTmpDirectory(this.getClass().getSimpleName()+"_"+testName.getMethodName()+"__tmpHierarchyForCommit");
		scmManipulator.checkout(checkoutDirectoryForVerifications);
		final File hello1 = new File(checkoutDirectoryForVerifications.getAbsolutePath()+"/jobs/hello.txt");
		final File hello2 = new File(checkoutDirectoryForVerifications.getAbsolutePath()+"/hello2.txt");
		FileUtils.fileAppend(hello1.getAbsolutePath(), "hello world !");
		FileUtils.fileAppend(hello2.getAbsolutePath(), "hello world 2 !");
		scmManipulator.addFile(checkoutDirectoryForVerifications, "jobs/hello.txt");
		scmManipulator.addFile(checkoutDirectoryForVerifications, "hello2.txt");
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, new ArrayList<File>(){{ add(hello1); add(hello2); }}, "external commit");

		
		// Deleting fakeJob
		Item mockedItem = Mockito.mock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onDeleted(mockedItem);
		
		// Assert no hello file is present in current hudson root
		assert !new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists();
		assert !new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists();
	}
}
