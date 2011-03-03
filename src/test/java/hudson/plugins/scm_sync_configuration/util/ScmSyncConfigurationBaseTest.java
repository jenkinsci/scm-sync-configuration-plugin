package hudson.plugins.scm_sync_configuration.util;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
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
import hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncSubversionSCM;
import hudson.plugins.test.utils.DirectoryUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, SCM.class, ScmSyncSubversionSCM.class, PluginWrapper.class})
public class ScmSyncConfigurationBaseTest {
	
	@Rule protected TestName testName = new TestName();
	private File currentTestDirectory = null;
	private File curentLocalSvnRepository = null;
	private File currentHudsonRootDirectory = null;

	@Before
	public void setup() throws Throwable {
		// Instantiating ScmSyncConfigurationPlugin instance
		ScmSyncConfigurationPlugin scmSyncConfigPluginInstance = new ScmSyncConfigurationPlugin();
		
		// Mocking PluginWrapper attached to current ScmSyncConfigurationPlugin instance
		PluginWrapper pluginWrapper = PowerMock.createMock(PluginWrapper.class);
		expect(pluginWrapper.getShortName()).andStubReturn("scm-sync-configuration");
		// Setting field on current plugin instance
		Field wrapperField = Plugin.class.getDeclaredField("wrapper");
		boolean wrapperFieldAccessibility = wrapperField.isAccessible();
		wrapperField.setAccessible(true);
		wrapperField.set(scmSyncConfigPluginInstance, pluginWrapper);
		wrapperField.setAccessible(wrapperFieldAccessibility);

		// Mocking Hudson root directory
		currentTestDirectory = createTmpDirectory("SCMSyncConfigTestsRoot");
		currentHudsonRootDirectory = new File(currentTestDirectory.getAbsolutePath()+"/hudsonRootDir/");
	    if(!(currentHudsonRootDirectory.mkdir())) { throw new IOException("Could not create hudson root directory: " + currentHudsonRootDirectory.getAbsolutePath()); }
		FileUtils.copyDirectoryStructure(new ClassPathResource(getHudsonRootBaseTemplate()).getFile(), currentHudsonRootDirectory);

        //EnvVars env = Computer.currentComputer().getEnvironment();
        //env.put("HUDSON_HOME", tmpHudsonRoot.getPath() );

		// Creating local SVN repository...
		curentLocalSvnRepository = new File(currentTestDirectory.getAbsolutePath()+"/svnLocalRepo/");
	    if(!(curentLocalSvnRepository.mkdir())) { throw new IOException("Could not create SVN local repo directory: " + curentLocalSvnRepository.getAbsolutePath()); }
	    FileUtils.copyDirectoryStructure(new ClassPathResource("svnEmptyRepository").getFile(), curentLocalSvnRepository);

	    // Mocking user
	    User mockedUser = EasyMock.createMock(User.class);
	    expect(mockedUser.getId()).andStubReturn("fcamblor");
	    
		// Mocking Hudson singleton instance ...
		mockStatic(Hudson.class);
		Hudson hudsonMockedInstance = createPartialMock(Hudson.class, new String[]{ "getRootDir", "getMe", "getPlugin" });
		expect(Hudson.getInstance()).andStubReturn(hudsonMockedInstance);
		expect(hudsonMockedInstance.getRootDir()).andStubReturn(currentHudsonRootDirectory);
		expect(hudsonMockedInstance.getMe()).andStubReturn(mockedUser);
		expect(hudsonMockedInstance.getPlugin(ScmSyncConfigurationPlugin.class)).andStubReturn(scmSyncConfigPluginInstance);
		
		replay(hudsonMockedInstance, pluginWrapper, mockedUser);
		replay(Hudson.class);
	}
	
	@After
	public void teardown() throws Throwable {
		// Deleting current test directory
		FileUtils.deleteDirectory(currentTestDirectory);
	}
	
	// Overridable
	protected String getHudsonRootBaseTemplate(){
		return "hudsonRootBaseTemplate/";
	}
	
	protected static File createTmpDirectory(String directoryPrefix) throws IOException {
	    final File temp = File.createTempFile(directoryPrefix, Long.toString(System.nanoTime()));
	    if(!(temp.delete())) { throw new IOException("Could not delete temp file: " + temp.getAbsolutePath()); }
	    if(!(temp.mkdir())) { throw new IOException("Could not create temp directory: " + temp.getAbsolutePath()); }
	    return (temp);
	}
	
	protected SCM createSCMMock(boolean withCredentials){
		
		List<String> partiallyMockedMethods = new ArrayList<String>();
		if(withCredentials){
			partiallyMockedMethods.add("extractScmCredentials");
		}
		
		SCM mockedSCM = createPartialMock(getSCMClass(), partiallyMockedMethods.toArray(new String[0]));
		mockStatic(SCM.class);
		
		expect(SCM.valueOf(notNull(String.class))).andReturn(mockedSCM);

		if(withCredentials){
			SCMCredentialConfiguration mockedCredential = new SCMCredentialConfiguration("toto");
			expect(mockedSCM.extractScmCredentials(notNull(String.class))).andReturn(mockedCredential).anyTimes();
		}
		
		replay(mockedSCM);
		replay(SCM.class);
		
		return mockedSCM;
	}

	protected SCMManipulator createMockedScmManipulator() throws ComponentLookupException, PlexusContainerException{
		// Settling up scm context
		SCM mockedSCM = createSCMMock(true);
		ScmContext scmContext = new ScmContext(mockedSCM, getSCMRepositoryURL());
		SCMManipulator scmManipulator = new SCMManipulator(SCMManagerFactory.getInstance().createScmManager());
		boolean configSettledUp = scmManipulator.scmConfigurationSettledUp(scmContext, true);
		assert configSettledUp;
		
		return scmManipulator;
	}
	
	protected void verifyCurrentScmContentMatchesHierarchy(String hierarchyPath) throws ComponentLookupException, PlexusContainerException, IOException{
		SCMManipulator scmManipulator = createMockedScmManipulator();
		
		// Checkouting scm in temp directory
		File checkoutDirectoryForVerifications = createTmpDirectory(this.getClass().getSimpleName()+"_"+testName.getMethodName()+"__verifyCurrentScmContentMatchesHierarchy");
		scmManipulator.checkout(checkoutDirectoryForVerifications);
		boolean directoryContentsAreEqual = DirectoryUtils.directoryContentsAreEqual(checkoutDirectoryForVerifications, new ClassPathResource(hierarchyPath).getFile(), 
				getSpecialSCMDirectoryExcludePattern(), true);
		
		FileUtils.deleteDirectory(checkoutDirectoryForVerifications);
		
		assert directoryContentsAreEqual;
	}
	
	// Overridable in a near future (when dealing with multiple scms ...)
	protected String getSCMRepositoryURL(){
		return "scm:svn:file:///"+this.getCurentLocalSvnRepository().getAbsolutePath();
	}
	
	// Overridable in a near future (when dealing with multiple scms ...)
	protected Pattern getSpecialSCMDirectoryExcludePattern(){
		return Pattern.compile("\\.svn");
	}

	// Overridable in a near future (when dealing with multiple scms ...)
	protected Class<? extends SCM> getSCMClass(){
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
	
	public File getCurrentScmSyncConfigurationCheckoutDirectory(){
		return new File(currentHudsonRootDirectory.getAbsolutePath()+"/scm-sync-configuration/checkoutConfiguration/");
	}
}
