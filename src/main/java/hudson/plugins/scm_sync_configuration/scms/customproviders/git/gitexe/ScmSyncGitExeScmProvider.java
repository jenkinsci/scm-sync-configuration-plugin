package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.scm.*;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.git.command.GitCommand;
import org.apache.maven.scm.provider.git.gitexe.GitExeScmProvider;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.scm.provider.git.gitexe.command.add.GitAddCommand;
import org.apache.maven.scm.provider.git.gitexe.command.branch.GitBranchCommand;
import org.apache.maven.scm.provider.git.gitexe.command.checkin.GitCheckInCommand;
import org.apache.maven.scm.provider.git.gitexe.command.status.GitStatusCommand;
import org.apache.maven.scm.provider.git.gitexe.command.status.GitStatusConsumer;
import org.apache.maven.scm.provider.git.repository.GitScmProviderRepository;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 */
public class ScmSyncGitExeScmProvider extends GitExeScmProvider {
    public static class ScmSyncGitCheckInCommand extends GitCheckInCommand {
        // Retrieved implementation from GitCheckInCommande v1.8.1, only overriding call to createPushCommandLine()
        // by a *custom* implementation
        @Override
        protected CheckInScmResult executeCheckInCommand(
                ScmProviderRepository repo, ScmFileSet fileSet, String message, ScmVersion version ) throws ScmException {

            GitScmProviderRepository repository = (GitScmProviderRepository) repo;

            CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
            CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();

            int exitCode;

            File messageFile = FileUtils.createTempFile("maven-scm-", ".commit", null);
            try
            {
                FileUtils.fileWrite( messageFile.getAbsolutePath(), message );
            }
            catch ( IOException ex )
            {
                return new CheckInScmResult( null, "Error while making a temporary file for the commit message: "
                    + ex.getMessage(), null, false );
            }

            try
            {
                if ( !fileSet.getFileList().isEmpty() )
                {
                    // if specific fileSet is given, we have to git-add them first
                    // otherwise we will use 'git-commit -a' later

                    Commandline clAdd = GitAddCommand.createCommandLine(fileSet.getBasedir(), fileSet.getFileList());

                    exitCode = GitCommandLineUtils.execute(clAdd, stdout, stderr, getLogger());

                    if ( exitCode != 0 )
                    {
                        return new CheckInScmResult( clAdd.toString(), "The git-add command failed.", stderr.getOutput(),
                                                     false );
                    }

                }

                // git-commit doesn't show single files, but only summary :/
                // so we must run git-status and consume the output
                // borrow a few things from the git-status command
                Commandline clStatus = GitStatusCommand.createCommandLine(repository, fileSet);

                GitStatusConsumer statusConsumer = new GitStatusConsumer( getLogger(), fileSet.getBasedir() );
                exitCode = GitCommandLineUtils.execute( clStatus, statusConsumer, stderr, getLogger() );
                if ( exitCode != 0 )
                {
                    // git-status returns non-zero if nothing to do
                    if ( getLogger().isInfoEnabled() )
                    {
                        getLogger().info( "nothing added to commit but untracked files present (use \"git add\" to " +
                                "track)" );
                    }
                }

                if ( statusConsumer.getChangedFiles().isEmpty() )
                {
                    return new CheckInScmResult( null, statusConsumer.getChangedFiles() );
                }

                Commandline clCommit = createCommitCommandLine( repository, fileSet, messageFile );

                exitCode = GitCommandLineUtils.execute( clCommit, stdout, stderr, getLogger() );
                if ( exitCode != 0 )
                {
                    return new CheckInScmResult( clCommit.toString(), "The git-commit command failed.", stderr.getOutput(),
                                                 false );
                }

                if( repo.isPushChanges() )
                {
                    Commandline cl = createSpecificPushCommandLine( getLogger(), repository, fileSet, version );

                    exitCode = GitCommandLineUtils.execute( cl, stdout, stderr, getLogger() );
                    if ( exitCode != 0 )
                    {
                        return new CheckInScmResult( cl.toString(), "The git-push command failed.", stderr.getOutput(), false );
                    }
                }

                List<ScmFile> checkedInFiles = new ArrayList<ScmFile>( statusConsumer.getChangedFiles().size() );

                // rewrite all detected files to now have status 'checked_in'
                for ( ScmFile changedFile : statusConsumer.getChangedFiles() )
                {
                    ScmFile scmfile = new ScmFile( changedFile.getPath(), ScmFileStatus.CHECKED_IN );

                    if ( fileSet.getFileList().isEmpty() )
                    {
                        checkedInFiles.add( scmfile );
                    }
                    else
                    {
                        // if a specific fileSet is given, we have to check if the file is really tracked
                        for ( File f : fileSet.getFileList() )
                        {
                            if ( FilenameUtils.separatorsToUnix(f.getPath()).equals( scmfile.getPath() ) )
                            {
                                checkedInFiles.add( scmfile );
                            }

                        }
                    }
                }

                return new CheckInScmResult( clCommit.toString(), checkedInFiles );
            }
            finally
            {
                try
                {
                    FileUtils.forceDelete( messageFile );
                }
                catch ( IOException ex )
                {
                    // ignore
                }
            }

        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

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

    @Override
    protected GitCommand getCheckInCommand() {
        return new ScmSyncGitCheckInCommand();
    }

}
