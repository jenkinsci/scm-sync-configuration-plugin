package hudson.plugins.scm_sync_configuration.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;

import hudson.model.Item;
import hudson.model.Job;
import hudson.plugins.scm_sync_configuration.SCMManipulator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.test.utils.scms.ScmUnderTestSubversion;


public class HudsonExtensionsSubversionTest extends HudsonExtensionsTest {

    public HudsonExtensionsSubversionTest() {
        super(new ScmUnderTestSubversion());
    }

    @Test
    public void shouldJobDeleteDoesntPerformAnyScmUpdate() throws Throwable {
        // Initializing the repository...
        createSCMMock();

        // Synchronizing hudson config files
        sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.getInstance().getEnabledStrategies());

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
        assertTrue("External check-in should succeed", scmManipulator.checkinFiles(checkoutDirectoryForVerifications, "external commit"));

        // Deleting fakeJob
        Item mockedItem = Mockito.mock(Job.class);
        File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/fakeJob/" );
        when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);

        sscItemListener.onDeleted(mockedItem);

        // Assert no hello file is present in current hudson root
        assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists(), is(false));
        assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists(), is(false));

        assertStatusManagerIsOk();
    }

    @Test
    public void shouldJobRenameDoesntPerformAnyScmUpdate() throws Throwable {
        // Initializing the repository...
        createSCMMock();

        // Synchronizing hudson config files
        sscBusiness.synchronizeAllConfigs(ScmSyncConfigurationPlugin.getInstance().getEnabledStrategies());

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
        Item mockedItem = Mockito.mock(Job.class);
        File mockedItemRootDir = new File(getCurrentHudsonRootDirectory() + "/jobs/newFakeJob/" );
        when(mockedItem.getRootDir()).thenReturn(mockedItemRootDir);

        sscItemListener.onLocationChanged(mockedItem, "fakeJob", "newFakeJob");

        // Assert no hello file is present in current hudson root
        assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/jobs/hello.txt").exists(), is(false));
        assertThat(new File(this.getCurrentScmSyncConfigurationCheckoutDirectory()+"/hello2.txt").exists(), is(false));

        assertStatusManagerIsOk();
    }


}
