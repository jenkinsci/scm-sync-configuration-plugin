package hudson.plugins.scm_sync_configuration.transactions;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;

/**
 * @author fcamblor
 */
public abstract class ScmTransaction {
    private ChangeSet changeset;

    protected ScmTransaction(){
        this.changeset = new ChangeSet();
    }

    public void defineCommitMessage(WeightedMessage weightedMessage){
        this.changeset.defineMessage(weightedMessage);
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
