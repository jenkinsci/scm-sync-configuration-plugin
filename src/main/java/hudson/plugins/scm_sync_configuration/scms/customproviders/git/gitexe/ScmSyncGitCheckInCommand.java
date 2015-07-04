package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.scm.provider.git.gitexe.command.branch.GitBranchCommand;
import org.apache.maven.scm.provider.git.gitexe.command.checkin.GitCheckInCommand;
import org.apache.maven.scm.provider.git.repository.GitScmProviderRepository;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * @author fcamblor
 * Crappy hack because for the moment, in maven-scmprovider-gitext 1.8.1, when we checkIn file,
 * checkin is originally made by passing pushUrl instead of a local reference (such as "origin")
 * Problem is passing pushUrl doesn't update current local reference once pushed => after push,
 * origin/<targetBranch> will not be updated to latest commit => on next push, there will be an
 * error saying some pull is needed.
 * This workaround could be betterly handled when something like "checkinAndFetch" could be
 * implemented generically in maven-scm-api
 * (see http://maven.40175.n5.nabble.com/SCM-GitExe-no-fetch-after-push-td5745064.html)
 * 
 * @author Tom <tw201207@gmail.com>
 * Rewritten executeCheckInCommand to account for SCM-772 and SCM-695. Make use of also fixed GitAddCommand
 * instead of re-inventing the wheel once more again.
 */
public class ScmSyncGitCheckInCommand extends GitCheckInCommand {
    // Retrieved implementation from GitCheckInCommande v1.8.1, only overriding call to createPushCommandLine()
    // by a *custom* implementation
    @Override
    protected CheckInScmResult executeCheckInCommand(
            ScmProviderRepository repo, ScmFileSet fileSet, String message, ScmVersion version ) throws ScmException {

        GitScmProviderRepository repository = (GitScmProviderRepository) repo;

        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();

        File messageFile = FileUtils.createTempFile("maven-scm-", ".commit", null);
        try {
            FileUtils.fileWrite( messageFile.getAbsolutePath(), message );
        } catch (IOException ex) {
            return new CheckInScmResult( null, "Error while making a temporary file for the commit message: "
                + ex.getMessage(), null, false );
        }

        try
        {
            // if specific fileSet is given, we have to git-add them first
            // otherwise we will use 'git-commit -a' later
            if ( !fileSet.getFileList().isEmpty() )
            {
            	ScmSyncGitAddCommand addCommand = new ScmSyncGitAddCommand();
            	addCommand.setLogger(getLogger());
            	AddScmResult addResult = addCommand.executeAddFileSet(fileSet);
            	if (addResult != null && addResult.isSuccess()) {
                    return new CheckInScmResult(new ArrayList<ScmFile>(), addResult);
                }
            }
            // Must run status here. There might have been earlier additions!!
    		ScmSyncGitStatusCommand statusCommand = new ScmSyncGitStatusCommand();
    		statusCommand.setLogger(getLogger());
    		StatusScmResult status = statusCommand.executeStatusCommand(repository, new ScmFileSet(fileSet.getBasedir()));
    		List<ScmFile> changedFiles = null;
    		if (status.isSuccess()) {
	    		changedFiles = status.getChangedFiles();
	            if (changedFiles.isEmpty()) {
	                return new CheckInScmResult(null, changedFiles);
	            }
    		} else {
    			return new CheckInScmResult(new ArrayList<ScmFile>(), status);
    		}
            Commandline clCommit = createCommitCommandLine(repository, fileSet, messageFile);
            int exitCode = GitCommandLineUtils.execute(clCommit, stdout, stderr, getLogger());
            if ( exitCode != 0 ) {
            	String msg = stderr.getOutput();
                return new CheckInScmResult(clCommit.toString(), "The git-commit command failed.", msg, false);
            }
            if (repo.isPushChanges()) {
                Commandline cl = createSpecificPushCommandLine( getLogger(), repository, fileSet, version );
                exitCode = GitCommandLineUtils.execute( cl, stdout, stderr, getLogger() );
                if ( exitCode != 0 ) {
                	String msg = stderr.getOutput();
                    return new CheckInScmResult(cl.toString(), "The git-push command failed.", msg, false);
                }
            }

            List<ScmFile> checkedInFiles = new ArrayList<ScmFile>( changedFiles.size() );

            // rewrite all detected files to now have status 'checked_in'
            for (ScmFile changedFile : changedFiles) {
                checkedInFiles.add( new ScmFile(changedFile.getPath(), ScmFileStatus.CHECKED_IN));
            }

            return new CheckInScmResult(clCommit.toString(), checkedInFiles);
        } finally {
            try {
                FileUtils.forceDelete( messageFile );
            } catch ( IOException ex ) {
                // ignore
            }
        }
    }

    public static Commandline createSpecificPushCommandLine( ScmLogger logger, GitScmProviderRepository repository,
                                                     ScmFileSet fileSet, ScmVersion version )
        throws ScmException
    {
        Commandline cl = GitCommandLineUtils.getBaseGitCommandLine( fileSet.getBasedir(), "push" );

        String branch = GitBranchCommand.getCurrentBranch(logger, repository, fileSet);

        if ( branch == null || branch.length() == 0 )
        {
            throw new ScmException( "Could not detect the current branch. Don't know where I should push to!" );
        }

        // Overloaded branch name here : if repository.getUrl() is kept, during checkin(), current *local* branch
        // reference is not updated, whereas by using origin, it will be done !
        cl.createArg().setValue( "origin" );

        cl.createArg().setValue( branch + ":" + branch );

        return cl;
    }

}