package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Job;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationItemListener;
import hudson.plugins.scm_sync_configuration.extensions.ScmSyncConfigurationSaveableListener;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationPluginBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTest;
import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.when;

/**
 * @author fcamblor
 */
public class JobConfigScmSyncStrategyTest extends ScmSyncConfigurationPluginBaseTest {

    private ScmSyncConfigurationSaveableListener sscConfigurationSaveableListener;

    public JobConfigScmSyncStrategyTest() {
        super(new ScmUnderTestSubversion());
    }

    @Before
   	public void initObjectsUnderTests() throws Throwable{
   		this.sscConfigurationSaveableListener = new ScmSyncConfigurationSaveableListener();
    }

    protected String getHudsonRootBaseTemplate(){
   		return "jobConfigStrategyTemplate/";
   	}

    // Reproducing JENKINS-17545
    @Test
    public void shouldConfigInSubmodulesNotSynced() throws ComponentLookupException, PlexusContainerException, IOException {
		// Initializing the repository...
		createSCMMock();

		// Synchronizing hudson config files
		sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.AVAILABLE_STRATEGIES);

        File subModuleConfigFile = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/modules/submodule/config.xml" );

        // Creating fake new item
        Job mockedItem = Mockito.mock(Job.class);
        when(mockedItem.getRootDir()).thenReturn(subModuleConfigFile.getParentFile());

        sscConfigurationSaveableListener.onChange(mockedItem, new XmlFile(subModuleConfigFile));

		verifyCurrentScmContentMatchesHierarchy("expected-scm-hierarchies/JobConfigScmSyncStrategyTest.shouldConfigInSubmodulesNotSynced/");

		assertStatusManagerIsOk();
    }

}
