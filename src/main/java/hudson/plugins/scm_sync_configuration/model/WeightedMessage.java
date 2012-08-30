package hudson.plugins.scm_sync_configuration.model;

/**
 * @author fcamblor
 */
public class WeightedMessage {
    String message;
    ChangeSet.MessageWeight weight;
    public WeightedMessage(String message, ChangeSet.MessageWeight weight) {
        this.message = message;
        this.weight = weight;
    }
    public String getMessage() {
        return message;
    }
    public ChangeSet.MessageWeight getWeight() {
        return weight;
    }
}
