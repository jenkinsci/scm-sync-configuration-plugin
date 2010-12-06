package hudson.plugins.scm_sync_configuration.util;

import static org.easymock.EasyMock.expect;

import java.io.File;
import java.io.IOException;

import hudson.model.Hudson;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.powermock.api.easymock.PowerMock.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Hudson.class)
public class ScmSyncConfigurationBaseTest {
	
	private File currentTestDirectory = null;
	private File curentLocalSvnRepository = null;

	@Before
	public void setup() throws Throwable {
		// Mocking Hudson singleton instance ...
		mockStatic(Hudson.class);
		Hudson hudsonMockedInstance = createPartialMock(Hudson.class, new String[]{ "getRootDir" });
		expect(Hudson.getInstance()).andStubReturn(hudsonMockedInstance);
		
		// Mocking Hudson root directory
		currentTestDirectory = createTmpDirectory();
		File tmpHudsonRoot = new File(currentTestDirectory.getAbsolutePath()+"/hudsonRootDir/");
	    if(!(tmpHudsonRoot.mkdir())) { throw new IOException("Could not create hudson root directory: " + tmpHudsonRoot.getAbsolutePath()); }
		expect(hudsonMockedInstance.getRootDir()).andStubReturn(tmpHudsonRoot);
		FileUtils.copyDirectoryStructure(new ClassPathResource("hudsonRootBaseTemplate/").getFile(), tmpHudsonRoot);
		
		// Creating local SVN repository...
		File curentLocalSvnRepository = new File(currentTestDirectory.getAbsolutePath()+"/svnLocalRepo/");
	    if(!(curentLocalSvnRepository.mkdir())) { throw new IOException("Could not create SVN local repo directory: " + curentLocalSvnRepository.getAbsolutePath()); }
	    FileUtils.copyDirectoryStructure(new ClassPathResource("svnEmptyRepository").getFile(), curentLocalSvnRepository);
		
		replayAll();
	}
	
	@After
	public void teardown() throws Throwable {
		// Deleting current test directory
		currentTestDirectory.delete();
	}
	
	protected static File createTmpDirectory() throws IOException {
	    final File temp = File.createTempFile("hudsonHomeRoot", Long.toString(System.nanoTime()));
	    if(!(temp.delete())) { throw new IOException("Could not delete temp file: " + temp.getAbsolutePath()); }
	    if(!(temp.mkdir())) { throw new IOException("Could not create temp directory: " + temp.getAbsolutePath()); }
	    return (temp);
	}

	protected File getCurrentTestDirectory() {
		return currentTestDirectory;
	}

	protected File getCurentLocalSvnRepository() {
		return curentLocalSvnRepository;
	}
}
