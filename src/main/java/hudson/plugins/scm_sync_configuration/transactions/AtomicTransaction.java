package hudson.plugins.scm_sync_configuration.transactions;

/**
 * This ScmTransaction implementation should be aimed at commiting changes immediately
 * @author fcamblor
 */
public class AtomicTransaction extends ScmTransaction {

    public AtomicTransaction(boolean synchronousCommit){
        super(synchronousCommit);
    }

    public AtomicTransaction(){
        super();
    }

    @Override
    public void registerPath(String path){
        super.registerPath(path);
        // We should commit transaction after every change
        commit();
    }

    @Override
    public void registerPathForDeletion(String path) {
        super.registerPathForDeletion(path);
        // We should commit transaction after every change
        commit();
    }

    @Override
    public void registerRenamedPath(String oldPath, String newPath){
        super.registerRenamedPath(oldPath, newPath);
        // We should commit transaction after every change
        commit();
    }

}
