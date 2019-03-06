package hudson.plugins.scm_sync_configuration.transactions;

/**
 * This ScmTransaction implementation should be aimed at memorizing every changes made during a thread
 * transaction, then commiting changes in the end of the thread
 * @author fcamblor
 */
public class ThreadedTransaction extends ScmTransaction {

    public ThreadedTransaction(boolean synchronousCommit){
        super(synchronousCommit);
    }

    public ThreadedTransaction(){
        super();
    }
}
