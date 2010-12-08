package hudson.plugins.scm_sync_configuration.util;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.powermock.api.easymock.PowerMock.createPartialMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.SCMCredentialConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, SCM.class})
public class ScmSyncConfigurationBaseTest {
	
	private File currentTestDirectory = null;
	private File curentLocalSvnRepository = null;

	@Before
	public void setup() throws Throwable {
		// Mocking Hudson root directory
		currentTestDirectory = createTmpDirectory("SCMSyncConfigTestsRoot");
		File tmpHudsonRoot = new File(currentTestDirectory.getAbsolutePath()+"/hudsonRootDir/");
	    if(!(tmpHudsonRoot.mkdir())) { throw new IOException("Could not create hudson root directory: " + tmpHudsonRoot.getAbsolutePath()); }
		FileUtils.copyDirectoryStructure(new ClassPathResource("hudsonRootBaseTemplate/").getFile(), tmpHudsonRoot);

        //EnvVars env = Computer.currentComputer().getEnvironment();
        //env.put("HUDSON_HOME", tmpHudsonRoot.getPath() );

		// Creating local SVN repository...
		curentLocalSvnRepository = new File(currentTestDirectory.getAbsolutePath()+"/svnLocalRepo/");
	    if(!(curentLocalSvnRepository.mkdir())) { throw new IOException("Could not create SVN local repo directory: " + curentLocalSvnRepository.getAbsolutePath()); }
	    FileUtils.copyDirectoryStructure(new ClassPathResource("svnEmptyRepository").getFile(), curentLocalSvnRepository);

		// Mocking Hudson singleton instance ...
		mockStatic(Hudson.class);
		Hudson hudsonMockedInstance = createPartialMock(Hudson.class, new String[]{ "getRootDir" });
		expect(Hudson.getInstance()).andStubReturn(hudsonMockedInstance);
		expect(hudsonMockedInstance.getRootDir()).andStubReturn(tmpHudsonRoot);

		replay(hudsonMockedInstance);
		replay(Hudson.class);
	}
	
	@After
	public void teardown() throws Throwable {
		// Deleting current test directory
		currentTestDirectory.delete();
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

	// Overridable in a near future (when dealing for multiple scms ..)
	protected Class<? extends SCM> getSCMClass(){
		return SCM.SUBVERSION.getClass();
	}
	
	protected File getCurrentTestDirectory() {
		return currentTestDirectory;
	}

	protected File getCurentLocalSvnRepository() {
		return curentLocalSvnRepository;
	}
}
