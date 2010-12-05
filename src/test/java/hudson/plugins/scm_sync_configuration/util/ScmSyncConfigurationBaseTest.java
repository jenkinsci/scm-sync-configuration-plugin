package hudson.plugins.scm_sync_configuration.util;

import static org.easymock.EasyMock.expect;
import hudson.model.Hudson;

import org.junit.Before;
import org.junit.runner.RunWith;
import static org.powermock.api.easymock.PowerMock.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Hudson.class)
public class ScmSyncConfigurationBaseTest {

	@Before
	public void setup() throws Throwable {
		mockStatic(Hudson.class);
		Hudson hudsonMockedInstance = createMock(Hudson.class);
		expect(Hudson.getInstance()).andStubReturn(hudsonMockedInstance);
		
		replay(Hudson.class);
	}
}
