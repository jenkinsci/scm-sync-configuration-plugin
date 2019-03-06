package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import java.io.File;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.scm.provider.git.gitexe.command.status.GitStatusCommand;
import org.apache.maven.scm.provider.git.repository.GitScmProviderRepository;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public class ScmSyncGitStatusCommand extends GitStatusCommand {

    @Override
    protected StatusScmResult executeStatusCommand(ScmProviderRepository repo, ScmFileSet fileSet) throws ScmException {
        Commandline cl = createCommandLine( (GitScmProviderRepository) repo, fileSet );
        File repoRootDirectory = ScmSyncGitUtils.getRepositoryRootDirectory(fileSet.getBasedir(), getLogger());
        FixedGitStatusConsumer consumer = new FixedGitStatusConsumer(getLogger(), fileSet.getBasedir(), repoRootDirectory);
        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
        int exitCode = GitCommandLineUtils.execute( cl, consumer, stderr, getLogger() );
        if (exitCode != 0) {
            if (getLogger().isInfoEnabled()) {
                getLogger().info( "nothing added to commit but untracked files present (use \"git add\" to track)" );
            }
        }
        return new StatusScmResult( cl.toString(), consumer.getChangedFiles() );
    }

    public static Commandline createCommandLine( GitScmProviderRepository repository, ScmFileSet fileSet )
    {
        Commandline cl = GitCommandLineUtils.getBaseGitCommandLine( fileSet.getBasedir(), "status" );
        cl.addArguments( new String[] { "--porcelain", "." } );
        return cl;
    }

}
