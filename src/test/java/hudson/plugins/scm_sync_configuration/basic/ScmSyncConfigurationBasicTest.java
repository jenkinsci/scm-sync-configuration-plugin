package hudson.plugins.scm_sync_configuration.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.util.ScmSyncConfigurationBaseTest;
import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;

import java.io.File;

import jenkins.model.Jenkins;

import org.junit.Test;

public class ScmSyncConfigurationBasicTest extends ScmSyncConfigurationBaseTest {

    public ScmSyncConfigurationBasicTest() {
        super(new ScmUnderTestSubversion());
    }

    @Test
    public void shouldRetrieveMockedHudsonInstanceCorrectly() throws Throwable {
        Jenkins jenkins = Jenkins.getInstance();
        assertNotNull("Jenkins instance must not be null", jenkins);
        assertFalse("Expected a mocked Jenkins instance", jenkins.getClass().equals(Jenkins.class) || jenkins.getClass().equals(Hudson.class));
    }

    @Test
    public void shouldVerifyIfHudsonRootDirectoryExists() throws Throwable {
        Jenkins jenkins = Jenkins.getInstance();
        File jenkinsRootDir = jenkins.getRootDir();
        assertNotNull("Jenkins instance must not be null", jenkinsRootDir);
        assertTrue("$JENKINS_HOME must be an existing directory", jenkinsRootDir.isDirectory());
    }

    @Test
    public void testPathesOutsideJenkisRoot () throws Exception {
        Jenkins jenkins = Jenkins.getInstance();
        File rootDirectory = jenkins.getRootDir().getAbsoluteFile();
        File parentDirectory = rootDirectory.getParentFile();
        assertNull("File outside $JENKINS_HOME should return null", JenkinsFilesHelper.buildPathRelativeToHudsonRoot(parentDirectory));
        assertNull("File outside $JENKINS_HOME should return null", JenkinsFilesHelper.buildPathRelativeToHudsonRoot(new File(parentDirectory, "foo.txt")));
    }

    @Test
    public void testPathesInsideJenkisRoot () throws Exception {
        Jenkins jenkins = Jenkins.getInstance();
        File rootDirectory = jenkins.getRootDir().getAbsoluteFile();
        File pathUnderTest = new File(rootDirectory, "config.xml");
        String result = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(pathUnderTest);
        assertNotNull("File inside $JENKINS_HOME must not return null path", result);
        assertEquals("Path " + pathUnderTest + " should resolve properly", result, "config.xml");
        pathUnderTest = new File(new File (rootDirectory, "someDir"), "foo.txt");
        result = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(pathUnderTest);
        assertNotNull("File inside $JENKINS_HOME must not return null path", result);
        assertEquals("Path " + pathUnderTest + " should resolve properly", result, "someDir/foo.txt");
    }
}
