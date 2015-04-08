package hudson.plugins.scm_sync_configuration.repository;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.TopLevelItem;
import hudson.plugins.scm_sync_configuration.SCMManipulator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationItemListener;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationSaveableListener;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.BasicPluginsConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JenkinsConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.impl.JobConfigScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTest;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		// Renaming fakeJob to newFakeJob
		Item mockedItem = Mockito.mock(TopLevelItem.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
        when(mockedItem.getName()).thenReturn("newFakeJob");

        // We should duplicate files in fakeJob to newFakeJob
        File oldJobDirectory = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/");
        FileUtils.copyDirectory(oldJobDirectory, mockedItemRootDir);

		sscItemListener.onRenamed(mockedItem, "fakeJob", "newFakeJob");
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobRenameBeCorrectlyImpactedOnSCM/");
		
		assertStatusManagerIsOk();
	}
	
	@Test
	public void shouldJobAddBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		File jobDirectory = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		File configFile = new File(jobDirectory.getAbsolutePath() + File.separator + "config.xml");
		jobDirectory.mkdir();
		FileUtils.copyFile(new ClassPathResource("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobAddBeCorrectlyImpactedOnSCM/jobs/newFakeJob/config.xml").getFile(), configFile);
		
		// Creating fake new job
		Item mockedItem = Mockito.mock(TopLevelItem.class);
		when(mockedItem.getRootDir()).thenReturn(jobDirectory);
		
		sscItemListener.onCreated(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/InitRepositoryTest.shouldSynchronizeHudsonFiles/");
		
		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobAddBeCorrectlyImpactedOnSCM/");
		
		assertStatusManagerIsOk();
	}
	
	@Test
	public void shouldJobModificationBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		File jobDirectory = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		File configFile = new File(jobDirectory.getAbsolutePath() + File.separator + "config.xml");
		
		// Creating fake new job
		Item mockedItem = Mockito.mock(TopLevelItem.class);
		when(mockedItem.getRootDir()).thenReturn(jobDirectory);
		
		sscItemListener.onCreated(mockedItem);

		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/InitRepositoryTest.shouldSynchronizeHudsonFiles/");
		
		FileUtils.copyFile(new ClassPathResource("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobModificationBeCorrectlyImpactedOnSCM/jobs/fakeJob/config.xml").getFile(), configFile);
		
		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobModificationBeCorrectlyImpactedOnSCM/");
		
		assertStatusManagerIsOk();
	}

	@Test
	public void shouldConfigModificationBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();

		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);

		File configFile = new File(getCurrentHudsonRootDirectory() + "/hudson.tasks.Shell.xml" );

		// Creating fake new job
		Item mockedItem = Mockito.mock(Item.class);
		when(mockedItem.getRootDir()).thenReturn(configFile);

		sscItemListener.onCreated(mockedItem);
		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));

		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/InitRepositoryTest.shouldSynchronizeHudsonFiles/");

		FileUtils.copyFile(new ClassPathResource("expected-scm-hierarchies/HudsonExtensionsTest.shouldConfigModificationBeCorrectlyImpactedOnSCM/hudson.tasks.Shell.xml").getFile(), configFile);

		sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(configFile));

		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldConfigModificationBeCorrectlyImpactedOnSCM/");

		assertStatusManagerIsOk();
	}

	@Test
	public void shouldJobRenameDoesntPerformAnyScmUpdate() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
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
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit");
		
		// Renaming fakeJob to newFakeJob
		Item mockedItem = Mockito.mock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onRenamed(mockedItem, "fakeJob", "newFakeJob");
		
		// Assert no hello file is present in current hudson root
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists(), is(false));
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists(), is(false));
		
		assertStatusManagerIsOk();
	}

	@Test
	public void shouldJobDeleteBeCorrectlyImpactedOnSCM() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		// Deleting fakeJob
		Item mockedItem = Mockito.mock(TopLevelItem.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onDeleted(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteBeCorrectlyImpactedOnSCM" + getSuffixForTestFiles() + "/");
		
		assertStatusManagerIsOk();
	}
		
	@Test
	public void shouldJobDeleteWithTwoJobsBeCorrectlyImpactedOnSCM() throws Throwable {
		String newFakeJob = "expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteWithTwoJobsBeCorrectlyImpactedOnSCM/jobs/newFakeJob";
		FileUtils.copyDirectoryStructure(new ClassPathResource(newFakeJob).getFile(), new File(getCurrentHudsonRootDirectory() + File.separator + "jobs" + File.separator + "newFakeJob"));
		
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		// Deleting fakeJob
		Item mockedItem = Mockito.mock(TopLevelItem.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onDeleted(mockedItem);
		
		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/HudsonExtensionsTest.shouldJobDeleteWithTwoJobsBeCorrectlyImpactedOnSCM/");
		
		assertStatusManagerIsOk();
	}

	@Test
	public void shouldJobDeleteDoesntPerformAnyScmUpdate() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
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
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit");
		
		// Deleting fakeJob
		Item mockedItem = Mockito.mock(Item.class);
		File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
		when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);
		
		sscItemListener.onDeleted(mockedItem);
		
		// Assert no hello file is present in current hudson root
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists(), is(false));
		assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists(), is(false));
		
		assertStatusManagerIsOk();
	}

	@Test
	public void shouldReloadAllFilesUpdateScmAndReloadAllFiles() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		// Let's checkout current scm view ... and commit something in it ...
		SCMManipulator scmManipulator = createMockedScmManipulator();
		File checkoutDirectoryForVerifications = createTmpDirectory(this.getClass().getSimpleName()+"_"+testName.getMethodName()+"__tmpHierarchyForCommit");
		scmManipulator.checkout(checkoutDirectoryForVerifications);
		
		verifyCurrentScmContentMatchesCurrentHudsonDir(true);
		
		final File configFile = new File(checkoutDirectoryForVerifications.getAbsolutePath() + "/config.xml");
		FileUtils.fileAppend(configFile.getAbsolutePath(), "toto");
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit on config file");
		
		final File configJobFile = new File(checkoutDirectoryForVerifications.getAbsolutePath() + "/jobs/fakeJob/config.xml");
		FileUtils.fileAppend(configJobFile.getAbsolutePath(), "titi");
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit on jonb file");
		
		verifyCurrentScmContentMatchesCurrentHudsonDir(false);
		
		// Reload config
		List<File> syncedFiles = sscBusiness.reloadAllFilesFromScm();
		
		verifyCurrentScmContentMatchesCurrentHudsonDir(true);
		
		assertThat(syncedFiles.size(), is(2));
		assertThat(syncedFiles.contains(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/config.xml")), is(true));
		assertThat(syncedFiles.contains(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/jobs/fakeJob/config.xml")), is(true));
		
		assertStatusManagerIsOk();
	}

	@Test
	public void shouldReloadAllFilesUpdateScmAndReloadAllFilesWithFileAdd() throws Throwable {
		// Initializing the repository...
		createSCMMock();
		
		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);
		
		// Let's checkout current scm view ... and commit something in it ...
		SCMManipulator scmManipulator = createMockedScmManipulator();
		File checkoutDirectoryForVerifications = createTmpDirectory(this.getClass().getSimpleName()+"_"+testName.getMethodName()+"__tmpHierarchyForCommit");
		scmManipulator.checkout(checkoutDirectoryForVerifications);

        // Verifying there isn't any difference between hudson and scm repo once every file are synchronized
		verifyCurrentScmContentMatchesCurrentHudsonDir(true);
		
		final File addedFile = new File(checkoutDirectoryForVerifications.getAbsolutePath() + "/myConfigFile.xml");
		FileUtils.fileWrite(addedFile.getAbsolutePath(), "toto");
		scmManipulator.addFile(checkoutDirectoryForVerifications, "myConfigFile.xml");
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit for add file");

		final String jobDir = checkoutDirectoryForVerifications.getAbsolutePath() + "/jobs/myJob";
		FileUtils.mkdir(jobDir);
		final File addedJobFile = new File(jobDir + "/config.xml");
		FileUtils.fileWrite(addedJobFile.getAbsolutePath(), "titi");
		scmManipulator.addFile(checkoutDirectoryForVerifications, "jobs/myJob");
		scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit for add job file");

		verifyCurrentScmContentMatchesCurrentHudsonDir(false);
		
		// Reload config
		List<File> syncedFiles = sscBusiness.reloadAllFilesFromScm();
		
		verifyCurrentScmContentMatchesCurrentHudsonDir(true);
		
		assertThat(syncedFiles.size(), is(2));
		assertThat(syncedFiles.contains(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/myConfigFile.xml")), is(true));
		assertThat(syncedFiles.contains(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/jobs/myJob")), is(true));
		
		assertStatusManagerIsOk();
	}

	@Test
	public void shouldFileWhichHaveToBeInSCM() throws Throwable {
        // IMPORTANT NOTE :
        // For every tested files in this test, file path should exist in
        // HudsonExtensionsTest.shouldFileWhichHaveToBeInSCM/ directory

		assertStrategy(JenkinsConfigScmSyncStrategy.class, Mockito.mock(Saveable.class), "config.xml");
		assertStrategy(BasicPluginsConfigScmSyncStrategy.class, Mockito.mock(Saveable.class), "hudson.scm.SubversionSCM.xml");
		assertStrategy(null, Mockito.mock(Saveable.class), "hudson.config.xml2");
		assertStrategy(null, Mockito.mock(Saveable.class), "nodeMonitors.xml");
		assertStrategy(null, Mockito.mock(Saveable.class), "toto" + File.separator + "hudson.config.xml");
		
		assertStrategy(null, Mockito.mock(Job.class), "toto" + File.separator + "config.xml");
		assertStrategy(null, Mockito.mock(Job.class), "jobs" + File.separator + "config.xml");
		assertStrategy(null, Mockito.mock(Saveable.class), "jobs" + File.separator + "myJob" + File.separator + "config.xml");
		assertStrategy(JobConfigScmSyncStrategy.class, Mockito.mock(TopLevelItem.class), "jobs" + File.separator + "myJob" + File.separator + "config.xml");
		assertStrategy(null, Mockito.mock(Job.class), "jobs" + File.separator + "myJob" + File.separator + "config2.xml");
	}

	private void assertStrategy(Class<? extends ScmSyncStrategy> expectedStrategyClass, Saveable saveableInstance, String targetPath) {
		ScmSyncStrategy strategy = ScmSyncConfigurationPlugin.getInstance().getStrategyForSaveable(saveableInstance, new File(getCurrentHudsonRootDirectory() + File.separator + targetPath));
		if (expectedStrategyClass == null) {
			assertThat(strategy, nullValue());
		}
		else {
			assertThat(strategy, notNullValue());
			assertThat(strategy, instanceOf(expectedStrategyClass));
		}
	}

    /**
     * Test that checks if after reload, disk contains only jobs that were reloaded from SCM. Speaking in other way -
     * if joba non existent in SCM were removed from the disk.
     * @throws Throwable
     */
    @Test
    public void shouldReloadAllFilesRemoveNonExistentJobs() throws Throwable {
        createSCMMock();

        sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);

        // Let's checkout current scm view ... and commit something in it ...
        SCMManipulator scmManipulator = createMockedScmManipulator();
        File checkoutDirectoryForVerifications = createTmpDirectory(this.getClass().getSimpleName()+"_"+testName.getMethodName()+"__tmpHierarchyForCommit");
        scmManipulator.checkout(checkoutDirectoryForVerifications);

        verifyCurrentScmContentMatchesCurrentHudsonDir(true);


        final String jobDir = checkoutDirectoryForVerifications.getAbsolutePath() + "/jobs/myJob";
        FileUtils.mkdir(jobDir);
        final File addedJobFile = new File(jobDir + "/config.xml");
        FileUtils.fileWrite(addedJobFile.getAbsolutePath(), "titi");
        scmManipulator.addFile(checkoutDirectoryForVerifications, "jobs/myJob");
        File jobToRemove = new File(checkoutDirectoryForVerifications.getAbsolutePath() + "/jobs/fakeJob");
        FileUtils.deleteDirectory(jobToRemove);
        scmManipulator.deleteHierarchy(jobToRemove);
        scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit for add myJob file and remove fakeJob");

        verifyCurrentScmContentMatchesCurrentHudsonDir(false);

        // Reload config
        List<File> removedJobs = sscBusiness.removeSourceJobsDuringReload();
        List<File> syncedFiles = sscBusiness.reloadAllFilesFromScm();

        verifyCurrentScmContentMatchesCurrentHudsonDir(true);

        assertThat(removedJobs.size(), is(1));
        assertThat(removedJobs.contains(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/jobs/fakeJob")), is(true));

        assertThat(syncedFiles.size(), is(1));
        assertThat(syncedFiles.contains(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/jobs/myJob")), is(true));

        assertThat(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/jobs/fakeJob").exists(), is(false));
        assertThat(new File(getCurrentHudsonRootDirectory().getAbsolutePath() + "/jobs/myJob").exists(), is(true));


        assertStatusManagerIsOk();
    }

    @Override
	protected String getHudsonRootBaseTemplate(){
        if("shouldFileWhichHaveToBeInSCM".equals(testName.getMethodName())){
            return "HudsonExtensionsTest.shouldFileWhichHaveToBeInSCM/";
        }

		return "hudsonRootBaseTemplate/";
	}
}
