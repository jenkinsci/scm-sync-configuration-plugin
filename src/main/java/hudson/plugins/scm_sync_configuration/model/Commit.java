package hudson.plugins.scm_sync_configuration.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import com.google.common.base.Strings;

import hudson.model.User;

/**
 * @author fcamblor
 * Commit is an aggregation of a changeset with a commit message
 * Note that commit message won't _always_ be the same as changeset.message since some additionnal contextual
 * informations will be provided.
 */
public class Commit {
    String message;
    ChangeSet changeset;
    ScmContext scmContext;
    User author;

    public Commit(ChangeSet changeset, User author, String userMessage, ScmContext scmContext) {
        this.message = createCommitMessage(scmContext, changeset.getMessage(), author, userMessage);
        this.changeset = changeset;
        this.scmContext = scmContext;
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public ChangeSet getChangeset() {
        return changeset;
    }

    public ScmContext getScmContext(){
        return scmContext;
    }

    private static String createCommitMessage(ScmContext context, String messagePrefix, User user, String userComment){
        StringBuilder commitMessage = new StringBuilder();
        if (user != null) {
            commitMessage.append(user.getId()).append(": ");
        }
        commitMessage.append(messagePrefix).append('\n');
        if (user != null) {
            commitMessage.append('\n').append("Change performed by ").append(user.getDisplayName()).append('\n');
        }
        if (userComment != null && !"".equals(userComment.trim())){
            commitMessage.append('\n').append(userComment.trim());
        }
        String message = commitMessage.toString();

        if (!Strings.isNullOrEmpty(context.getCommitMessagePattern())) {
            message = context.getCommitMessagePattern().replaceAll("\\[message\\]", message.replaceAll("\\$", "\\\\\\$"));
        }
        return wrapText(message, 72);
    }

    private static String wrapText(String str, int lineLength) {
        if (str == null) {
            return null;
        }
        int i = 0;
        int max = str.length();
        StringBuilder text = new StringBuilder();
        while (i < max) {
            int next = str.indexOf('\n', i);
            if (next < 0) {
                next = max;
            }
            String line = StringUtils.stripEnd(str.substring(i, next), null);
            if (line.length() > lineLength) {
                line = WordUtils.wrap(line, lineLength, "\n", false);
            }
            text.append(line).append('\n');
            i = next+1;
        }
        return text.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Commit %s : %n", super.toString()));
        sb.append(String.format("  Author : %s%n", String.valueOf(author)));
        sb.append(String.format("  Comment : %s%n", message));
        sb.append(String.format("  Changeset : %n%s%n", changeset.toString()));
        return sb.toString();
    }
}
