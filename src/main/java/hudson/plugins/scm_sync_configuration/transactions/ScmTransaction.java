package hudson.plugins.scm_sync_configuration.transactions;

import java.io.File;
import java.util.concurrent.Future;

import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;

/**
 * @author fcamblor
 */
public abstract class ScmTransaction {
    private final ChangeSet changeset;
    // Flag allowing to say if transaction will be asynchronous (default) or synchronous
    // Synchronous commit are useful during tests execution
    private final boolean synchronousCommit;

    protected ScmTransaction(){
        this(false);
    }

    protected ScmTransaction(boolean synchronousCommit){
        this.changeset = new ChangeSet();
        this.synchronousCommit = synchronousCommit;
    }

    public void defineCommitMessage(WeightedMessage weightedMessage){
        this.changeset.defineMessage(weightedMessage);
    }

    public void commit(){
        Future<Void> future = ScmSyncConfigurationPlugin.getInstance().commitChangeset(changeset);
        if (synchronousCommit && future != null) {
            // Synchronous transactions should wait for the future to be fully processed
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void registerPath(String path){
        this.changeset.registerPath(path);
    }

    public void registerPathForDeletion(String path) {
        this.changeset.registerPathForDeletion(path);
    }

    public void registerRenamedPath(String oldPath, String newPath){
        File newFile = JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(newPath);
        if (newFile.isDirectory()) {
            for (File f : ScmSyncConfigurationPlugin.getInstance().collectAllFilesForScm(newFile)) {
                String pathRelativeToRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(f);
                if (pathRelativeToRoot != null) {
                    this.changeset.registerPath(pathRelativeToRoot);
                }
            }
        } else {
            this.changeset.registerPath(newPath);
        }
        if (oldPath != null) {
            this.changeset.registerPathForDeletion(oldPath);
        }
    }
}
