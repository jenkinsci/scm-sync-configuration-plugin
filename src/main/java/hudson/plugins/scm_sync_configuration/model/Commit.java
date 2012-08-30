package hudson.plugins.scm_sync_configuration.model;

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
   		commitMessage.append(messagePrefix);
   		if(user != null){
   			commitMessage.append(" by ").append(user.getId());
   		}
   		if(userComment != null){
   			commitMessage.append(" with following comment : ").append(userComment);
   		}
   		String message = commitMessage.toString();

           if(context.getCommitMessagePattern() == null || "".equals(context.getCommitMessagePattern())){
               return message;
           } else {
               return context.getCommitMessagePattern().replaceAll("\\[message\\]", message.replaceAll("\\$", "\\\\\\$"));
           }
   	}
}
