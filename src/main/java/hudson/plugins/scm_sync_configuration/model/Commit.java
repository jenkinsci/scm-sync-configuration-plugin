package hudson.plugins.scm_sync_configuration.model;

/**
 * @author fcamblor
 * Commit is an aggregation of a changeset with a commit message
 * Note that commit message won't _always_ be the same as changeset.message since some additionnal contextual
 * informations will be provided.
 */
public class Commit {
    String message;
    ChangeSet changeset;

    public Commit(String message, ChangeSet changeset) {
        this.message = message;
        this.changeset = changeset;
    }

    public String getMessage() {
        return message;
    }

    public ChangeSet getChangeset() {
        return changeset;
    }
}
