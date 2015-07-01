package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import java.io.File;
import java.util.List;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.scm.provider.git.gitexe.command.remove.GitRemoveCommand;
import org.apache.maven.scm.provider.git.gitexe.command.remove.GitRemoveConsumer;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Yet another crappy hack to fix maven's gitexe implementation. It doesn't pass "--" to git rm,
 * leading to failures if a file starting with a dash is to be removed. Because of the poor design
 * of that library using static methods galore, we cannot just override the wrong method...
 */
public class ScmSyncGitRemoveCommand extends GitRemoveCommand {
	// Implementation copied from v1.9.1; single change in createCommandLine below.
	
    protected ScmResult executeRemoveCommand( ScmProviderRepository repo, ScmFileSet fileSet, String message )
        throws ScmException
    {

        if ( fileSet.getFileList().isEmpty() )
        {
            throw new ScmException( "You must provide at least one file/directory to remove" );
        }

        Commandline cl = createCommandLine( fileSet.getBasedir(), fileSet.getFileList() );

        GitRemoveConsumer consumer = new GitRemoveConsumer( getLogger() );

        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        int exitCode;

        exitCode = GitCommandLineUtils.execute( cl, consumer, stderr, getLogger() );
        if ( exitCode != 0 )
        {
            return new RemoveScmResult( cl.toString(), "The git command failed.", stderr.getOutput(), false );
        }

        return new RemoveScmResult( cl.toString(), consumer.getRemovedFiles() );
    }

    public static Commandline createCommandLine( File workingDirectory, List<File> files )
        throws ScmException
    {
        Commandline cl = GitCommandLineUtils.getBaseGitCommandLine( workingDirectory, "rm" );

        for ( File file : files )
        {
            if ( file.isAbsolute() )
            {
                if ( file.isDirectory() )
                {
                    cl.createArg().setValue( "-r" );
                    break;
                }
            }
            else
            {
                File absFile = new File( workingDirectory, file.getPath() );
                if ( absFile.isDirectory() )
                {
                    cl.createArg().setValue( "-r" );
                    break;
                }
            }
        }

        cl.createArg().setValue( "--" ); // This is missing upstream.
        
        GitCommandLineUtils.addTarget( cl, files );

        return cl;
    }
	
}
