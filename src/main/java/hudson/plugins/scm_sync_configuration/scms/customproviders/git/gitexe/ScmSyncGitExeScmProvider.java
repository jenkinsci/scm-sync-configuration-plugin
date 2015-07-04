package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import org.apache.maven.scm.provider.git.command.GitCommand;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;

/**
 * Try to fix those very broken maven scm git commands. We should really move to using the git-client plugin.
 */
public class ScmSyncGitExeScmProvider extends GitExeScmProvider {
    
	@Override
    protected GitCommand getCheckInCommand() {
		// Push to origin (fcamblor)
    	// Handle quoted output from git, and fix relative path computations SCM-695, SCM-772
        return new ScmSyncGitCheckInCommand();
    }

    @Override
    protected GitCommand getRemoveCommand() {
    	// Include -- in git rm
    	return new ScmSyncGitRemoveCommand();
    }
    
    @Override
    protected GitCommand getStatusCommand() {
    	// Handle quoted output from git, and fix relative path computations SCM-695, SCM-772
    	return new ScmSyncGitStatusCommand();
    }
    
    @Override
    protected GitCommand getAddCommand() {
    	// Handle quoted output from git, and fix relative path computations SCM-695, SCM-772
    	return new ScmSyncGitAddCommand();
    }
    
    // TODO: we also use checkout and update. Those call git ls-files, which parses the result wrongly...
    // (doesn't account for the partial escaping done there for \t, \n, and \\ .)
}
