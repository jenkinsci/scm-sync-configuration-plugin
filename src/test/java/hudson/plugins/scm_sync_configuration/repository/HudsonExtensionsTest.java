package hudson.plugins.scm_sync_configuration.repository;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.plugins.scm_sync_configuration.SCMManipulator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationItemListener;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationSaveableListener;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JenkinsConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JobConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTest;

import java.io.File;
import java.util.ArrayList;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.core.io.ClassPathResource;

@PrepareForTest(ScmSyncConfigurationPlugin.class)
public abstract class HudsonExtensionsTest extends ScmSyncConfigurationPluginBaseTest {

	private ScmSyncConfigurationItemListener sscItemListener;
	private ScmSyncConfigurationSaveableListener sscConfigurationSaveableListener;
	
	protected HudsonExtensionsTest(ScmUnderTest scmUnderTest) {
		super(scmUnderTest);
	}

	@Before
	public void initObjectsUnderTests() throws Throwable{
		this.sscItemListener = new ScmSyncConfigurationItemListener();
		this.sscConfigurationSaveableListener = new ScmSyncConfigurationSaveableListener();

		// Mocking ScmSyncConfigurationPlugin.getStrategyForSaveable()
		ScmSyncConfigurationPlugin sscPlugin = spy(ScmSyncConfigurationPlugin.getInstance());
		sscPlugin.setBusiness(this.sscBusiness);
		PowerMockito.doReturn(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES[0]).when(sscPlugin).getStrategyForSaveable(Mockito.any(Saveable.class), Mockito.any(File.class));
	}
	
	@Test
	public void shouldJobRenameBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		// Renaming fakeJob to newFakeJob
		Item mockedItem = Mockito.mock(Job.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onRenamed(mockedItem, "fakeJob", "newFakeJob");
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobRenameBeCorrectlyImpactedOnSCM/");
	}
	
	@Test
	public void shouldJobAddBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		File jobDirectory = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		File configFile = new File(jobDirectory.getAbsolutePath() + File.separator + "config.xml");
		jobDirectory.mkdir();
		FileUtils.copyFile(new ClassPathResource("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobAddBeCorrectlyImpactedOnSCM/jobs/newFakeJob/config.xml").getFile(), configFile);
		
		// Creating fake new job
		Item mockedItem = Mockito.mock(Job.class);
		when(mockedItem.getRootDir()).thenReturn(jobDirectory);
		
		sscItemListener.onCreated(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/InitRepositoryTest.shouldSynchronizeHudsonFiles/");
		
		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobAddBeCorrectlyImpactedOnSCM/");
	}
	
	@Test
	public void shouldJobModificationBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		File jobDirectory = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		File configFile = new File(jobDirectory.getAbsolutePath() + File.separator + "config.xml");
		
		// Creating fake new job
		Item mockedItem = Mockito.mock(Job.class);
		when(mockedItem.getRootDir()).thenReturn(jobDirectory);
		
		sscItemListener.onCreated(mockedItem);
		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/InitRepositoryTest.shouldSynchronizeHudsonFiles/");
		
		FileUtils.copyFile(new ClassPathResource("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobModificationBeCorrectlyImpactedOnSCM/jobs/fakeJob/config.xml").getFile(), configFile);
		
		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobModificationBeCorrectlyImpactedOnSCM/");
	}

	@Test
	public void shouldJobRenameDoesntPerformAnyScmUpdate() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
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
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists(), is(false));
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists(), is(false));
	}

	@Test
	public void shouldJobDeleteBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		// Deleting fakeJob
		Item mockedItem = Mockito.mock(Job.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onDeleted(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteBeCorrectlyImpactedOnSCM" + getSuffixForTestFiles() + "/");
	}
	
	@Test
	public void shouldJobDeleteWithTwoJobsBeCorrectlyImpactedOnSCM() throws Throwable {
		String newFakeJob = "expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteWithTwoJobsBeCorrectlyImpactedOnSCM/jobs/newFakeJob";
		FileUtils.copyDirectoryStructure(new ClassPathResource(newFakeJob).getFile(), new File(getCurrentHudsonRootDirectory() + File.separator + "jobs" + File.separator + "newFakeJob"));
		
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(scmContext, ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES, Hudson.getInstance().getMe());
		
		// Deleting fakeJob
		Item mockedItem = Mockito.mock(Job.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onDeleted(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteWithTwoJobsBeCorrectlyImpactedOnSCM/");
	}

	@Test
	public void shouldJobDeleteDoesntPerformAnyScmUpdate() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
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
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists(), is(false));
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists(), is(false));
	}

	private void assertStrategy(Class<? extends ScmSyncStrategy> clazz, Saveable object, String relativePath) {
		ScmSyncStrategy strategy = ScmSyncConfigurationPlugin.getInstance().getStrategyForSaveable(object, new File(getCurrentHudsonRootDirectory() + File.separator + relativePath));
		if (clazz == null) {
			assertThat(strategy, nullValue());
		}
		else {
			assertThat(strategy, notNullValue());
			assertThat(strategy, instanceOf(clazz));
		}
	}
	
}
