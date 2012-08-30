package hudson.plugins.scm_sync_configuration.model;

/**
 * @author fcamblor
 * WeightMessage is used to define a message with a weight
 * Weight will be used to prioritize commit messages into a ScmTransaction, in order to
 * have only the more important commit message kept during the transaction
 */
public class WeightedMessage {
    String message;
    MessageWeight weight;
    public WeightedMessage(String message, MessageWeight weight) {
        this.message = message;
        this.weight = weight;
    }
    public String getMessage() {
        return message;
    }
    public MessageWeight getWeight() {
        return weight;
    }
}
