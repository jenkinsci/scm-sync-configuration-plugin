package hudson.plugins.scm_sync_configuration.model;

/**
 * @author fcamblor
 * Message weight should be used to prioritize messages into a Scm Transaction
 */
public enum MessageWeight {
    MINIMAL(0),NORMAL(1),IMPORTANT(2),MORE_IMPORTANT(3);

    private int weight;
    private MessageWeight(int _weight){
        this.weight = _weight;
    }
    public boolean weighterThan(MessageWeight ms){
        return this.weight > ms.weight;
    }
}
