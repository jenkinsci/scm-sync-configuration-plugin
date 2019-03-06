package hudson.plugins.scm_sync_configuration.model;

/**
 * @author fcamblor
 * Message weight should be used to prioritize messages into a Scm Transaction
 */
public enum MessageWeight {
    MINIMAL,
    NORMAL,
    IMPORTANT,
    MORE_IMPORTANT;
}
