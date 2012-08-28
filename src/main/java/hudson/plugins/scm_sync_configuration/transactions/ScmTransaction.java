package hudson.plugins.scm_sync_configuration.transactions;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;

/**
 * @author fcamblor
 */
public abstract class ScmTransaction {
    private ChangeSet changeset;

    protected ScmTransaction(){
        this.changeset = new ChangeSet();
    }

    public void defineCommitMessage(String message, ChangeSet.MessageWeight weight){
        this.changeset.defineMessage(message, weight);
    }

    public void commit(){
        ScmSyncConfigurationPlugin.getInstance().commitChangeset(changeset);
    }

    public void registerPath(String path){
        this.changeset.registerPath(path);
    }

    public void registerPathForDeletion(String path) {
        this.changeset.registerPathForDeletion(path);
    }

    public void registerRenamedPath(String oldPath, String newPath){
        this.changeset.registerRenamedPath(oldPath, newPath);
    }
}
