package hudson.plugins.scm_sync_configuration.util;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.SCMManagerFactory;
import hudson.plugins.scm_sync_configuration.SCMManipulator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.SCMCredentialConfiguration;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncSubversionSCM;
import hudson.plugins.test.utils.DirectoryUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.objenesis.ObjenesisStd;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, SCM.class, ScmSyncSubversionSCM.class, PluginWrapper.class})
public class ScmSyncConfigurationBaseTest {
	
	protected static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationBaseTest.class.getName());
	@Rule
	protected TestName testName = new TestName();
	private File currentTestDirectory = null;
	private File curentLocalSvnRepository = null;
	private File currentHudsonRootDirectory = null;

	@Before
	public void setup() throws Throwable {
		// Instantiating ScmSyncConfigurationPlugin instance
		ScmSyncConfigurationPlugin scmSyncConfigPluginInstance = new ScmSyncConfigurationPlugin();
		
		// Mocking PluginWrapper attached to current ScmSyncConfigurationPlugin instance
		PluginWrapper pluginWrapper = PowerMockito.mock(PluginWrapper.class);
		when(pluginWrapper.getShortName()).thenReturn("scm-sync-configuration");
		// Setting field on current plugin instance
		Field wrapperField = Plugin.class.getDeclaredField("wrapper");
		boolean wrapperFieldAccessibility = wrapperField.isAccessible();
		wrapperField.setAccessible(true);
		wrapperField.set(scmSyncConfigPluginInstance, pluginWrapper);
		wrapperField.setAccessible(wrapperFieldAccessibility);

		// Mocking Hudson root directory
		currentTestDirectory = createTmpDirectory("SCMSyncConfigTestsRoot");
		currentHudsonRootDirectory = new File(currentTestDirectory.getAbsolutePath() + "/hudsonRootDir/");
		if (!(currentHudsonRootDirectory.mkdir())) {
			throw new IOException("Could not create hudson root directory: " + currentHudsonRootDirectory.getAbsolutePath());
		}
		FileUtils.copyDirectoryStructure(new ClassPathResource(getHudsonRootBaseTemplate()).getFile(), currentHudsonRootDirectory);

		//EnvVars env = Computer.currentComputer().getEnvironment();
		//env.put("HUDSON_HOME", tmpHudsonRoot.getPath() );

		// Creating local SVN repository...
		curentLocalSvnRepository = new File(currentTestDirectory.getAbsolutePath() + "/svnLocalRepo/");
		if (!(curentLocalSvnRepository.mkdir())) {
			throw new IOException("Could not create SVN local repo directory: " + curentLocalSvnRepository.getAbsolutePath());
		}
		FileUtils.copyDirectoryStructure(new ClassPathResource("svnEmptyRepository").getFile(), curentLocalSvnRepository);

		// Mocking user
		User mockedUser = Mockito.mock(User.class);
		when(mockedUser.getId()).thenReturn("fcamblor");
		
		// Mocking Hudson singleton instance ...
		// Warning : this line will only work on Objenesis supported VMs :
		// http://code.google.com/p/objenesis/wiki/ListOfCurrentlySupportedVMs
		Hudson hudsonMockedInstance = spy((Hudson) new ObjenesisStd().getInstantiatorOf(Hudson.class).newInstance());
		PowerMockito.doReturn(currentHudsonRootDirectory).when(hudsonMockedInstance).getRootDir();
		PowerMockito.doReturn(mockedUser).when(hudsonMockedInstance).getMe();
		PowerMockito.doReturn(scmSyncConfigPluginInstance).when(hudsonMockedInstance).getPlugin(ScmSyncConfigurationPlugin.class);
		
		PowerMockito.mockStatic(Hudson.class);
		PowerMockito.doReturn(hudsonMockedInstance).when(Hudson.class);
		Hudson.getInstance();
		//when(Hudson.getInstance()).thenReturn(hudsonMockedInstance);
	}
	
	@After
	public void teardown() throws Throwable {
		// Deleting current test directory
		try {
			FileUtils.deleteDirectory(currentTestDirectory);
		} catch (IOException ex) {
			// Windows does not allow to delete this folder
			LOGGER.throwing(FileUtils.class.getName(), "deleteDirectory", ex);
		}
	}
	
	// Overridable
	protected String getHudsonRootBaseTemplate() {
		return "hudsonRootBaseTemplate/";
	}
	
	protected static File createTmpDirectory(String directoryPrefix) throws IOException {
		final File temp = File.createTempFile(directoryPrefix, Long.toString(System.nanoTime()));
		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}
		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return (temp);
	}
	
	protected SCM createSCMMock(boolean withCredentials) {
	
		SCM mockedSCM = spy(SCM.valueOf(getSCMClass().getName()));
		
		if (withCredentials) {
			SCMCredentialConfiguration mockedCredential = new SCMCredentialConfiguration("toto");
			PowerMockito.doReturn(mockedCredential).when(mockedSCM).extractScmCredentials((String) Mockito.notNull());
		}
		
		return mockedSCM;
	}

	protected SCMManipulator createMockedScmManipulator() throws ComponentLookupException, PlexusContainerException {
		// Settling up scm context
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL(), getSCMCommentPrefix(), getSCMCommentSuffix());
		SCMManipulator scmManipulator = new SCMManipulator(SCMManagerFactory.getInstance().createScmManager());
		boolean configSettledUp = scmManipulator.scmConfigurationSettledUp(scmContext, true);
		assert configSettledUp;
		
		return scmManipulator;
	}
	
	protected void verifyCurrentScmContentMatchesHierarchy(String hierarchyPath) throws ComponentLookupException, PlexusContainerException, IOException {
		SCMManipulator scmManipulator = createMockedScmManipulator();
		
		// Checkouting scm in temp directory
		File checkoutDirectoryForVerifications = createTmpDirectory(this.getClass().getSimpleName() + "_" + testName.getMethodName() + "__verifyCurrentScmContentMatchesHierarchy");
		scmManipulator.checkout(checkoutDirectoryForVerifications);
		boolean directoryContentsAreEqual = DirectoryUtils.directoryContentsAreEqual(checkoutDirectoryForVerifications, new ClassPathResource(hierarchyPath).getFile(),
				getSpecialSCMDirectoryExcludePattern(), true);
		
		
		try {
			FileUtils.deleteDirectory(checkoutDirectoryForVerifications);
		} catch (IOException ex) {
			// Windows does not allow to delete this folder
			LOGGER.throwing(FileUtils.class.getName(), "deleteDirectory", ex);
		}
		
		assert directoryContentsAreEqual;
	}
	
	// Overridable in a near future (when dealing with multiple scms ...)
	protected String getSCMRepositoryURL() {
		return "scm:svn:file:///" + this.getCurentLocalSvnRepository().getAbsolutePath();
	}

	protected String getSCMCommentPrefix() {
		return "[Prefix]";
	}

	protected String getSCMCommentSuffix() {
		return "\nIssue #123";
	}
	
	// Overridable in a near future (when dealing with multiple scms ...)
	protected Pattern getSpecialSCMDirectoryExcludePattern() {
		return Pattern.compile("\\.svn");
	}

	// Overridable in a near future (when dealing with multiple scms ...)
	protected Class<? extends SCM> getSCMClass() {
		return ScmSyncSubversionSCM.class;
	}
	
	protected File getCurrentTestDirectory() {
		return currentTestDirectory;
	}

	protected File getCurentLocalSvnRepository() {
		return curentLocalSvnRepository;
	}

	public File getCurrentHudsonRootDirectory() {
		return currentHudsonRootDirectory;
	}
	
	public File getCurrentScmSyncConfigurationCheckoutDirectory() {
		return new File(currentHudsonRootDirectory.getAbsolutePath() + "/scm-sync-configuration/checkoutConfiguration/");
	}
}
