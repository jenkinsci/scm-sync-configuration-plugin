package hudson.plugins.scm_sync_configuration.util;

import static org.easymock.EasyMock.expect;

import java.io.File;
import java.io.IOException;

import hudson.model.Hudson;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.powermock.api.easymock.PowerMock.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Hudson.class)
public class ScmSyncConfigurationBaseTest {
	
	private File tmpHudsonRootDir = null;

	@Before
	public void setup() throws Throwable {
		mockStatic(Hudson.class);
		Hudson hudsonMockedInstance = createPartialMock(Hudson.class, new String[]{ "getRootDir" });
		expect(Hudson.getInstance()).andStubReturn(hudsonMockedInstance);
		
		tmpHudsonRootDir = createTmpDirectory();
		expect(hudsonMockedInstance.getRootDir()).andStubReturn(tmpHudsonRootDir);
		//expect(hudsonMockedInstance.getRootDir()).andThrow(new RuntimeException("aie !"));
		
		replayAll();
	}
	
	@After
	public void teardown() throws Throwable {
		// Deleting hudson root directory
		tmpHudsonRootDir.delete();
		
	}
	
	protected static File createTmpDirectory() throws IOException {
	    final File temp = File.createTempFile("hudsonHomeRoot", Long.toString(System.nanoTime()));
	    if(!(temp.delete())) { throw new IOException("Could not delete temp file: " + temp.getAbsolutePath()); }
	    if(!(temp.mkdir())) { throw new IOException("Could not create temp directory: " + temp.getAbsolutePath()); }
	    return (temp);
	}
}
