package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import org.apache.maven.scm.provider.git.command.GitCommand;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.codehaus.plexus.util.Os;

/**
 * Try to fix some very broken maven scm git commands.
 */
public class ScmSyncGitExeScmProvider extends GitExeScmProvider {
    
	@Override
    protected GitCommand getCheckInCommand() {
		// Push to origin
        return new ScmSyncGitCheckInCommand();
    }

    @Override
    protected GitCommand getRemoveCommand() {
    	// Include -- in git rm
    	return new ScmSyncGitRemoveCommand();
    }
    
    // More hacks. The command line library used by the gitexe in maven scm does not automatically quote
    // arguments containing blanks or other funny characters.
    //
    // Now this is going to be fun.
    //
    // On Unices, we need to protect both $ and blanks: single quote, and escape and single quote inside
    // On Windows, we need to protect % and blanks: double the %'s and then double quote.
    protected static String quote(String s) {
    	String quoteChar = "'";
    	if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    		if (s.indexOf('%') >= 0) {
    			s = s.replaceAll("%", "%%");
    		}
    		quoteChar="\"";
    	}
    	if (s.indexOf(quoteChar) >= 0) {
    		return quoteChar + s.replaceAll(quoteChar, '\\' + quoteChar) + quoteChar;
    	}
    	return s;
    }

}
