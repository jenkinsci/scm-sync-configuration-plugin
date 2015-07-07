package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.apache.maven.scm.provider.git.gitexe.command.add.GitAddCommand;
import org.apache.maven.scm.provider.git.repository.GitScmProviderRepository;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public class ScmSyncGitAddCommand extends GitAddCommand {

    @Override
    protected AddScmResult executeAddCommand(ScmProviderRepository repo, ScmFileSet fileSet, String message, boolean binary)
            throws ScmException {
        GitScmProviderRepository repository = (GitScmProviderRepository) repo;

        if (fileSet.getFileList().isEmpty()) {
            throw new ScmException("You must provide at least one file/directory to add");
        }

        AddScmResult result = executeAddFileSet(fileSet);

        if (result != null) {
            return result;
        }

        ScmSyncGitStatusCommand statusCommand = new ScmSyncGitStatusCommand();
        statusCommand.setLogger(getLogger());
        StatusScmResult status = statusCommand.executeStatusCommand(repository, fileSet);
        getLogger().warn("add - status - " + status.isSuccess());
        for (ScmFile s : status.getChangedFiles()) {
            getLogger().warn("added " + s.getPath());
        }
        List<ScmFile> changedFiles = new ArrayList<ScmFile>();

        if (fileSet.getFileList().isEmpty()) {
            changedFiles = status.getChangedFiles();
        } else {
            for (ScmFile scmfile : status.getChangedFiles()) {
                // if a specific fileSet is given, we have to check if the file is really tracked
                for (File f : fileSet.getFileList()) {
                    if (FilenameUtils.separatorsToUnix(f.getPath()).equals(scmfile.getPath())) {
                        changedFiles.add(scmfile);
                    }
                }
            }
        }
        Commandline cl = createCommandLine(fileSet.getBasedir(), fileSet.getFileList());
        return new AddScmResult(cl.toString(), changedFiles);
    }

    public static Commandline createCommandLine(File workingDirectory, List<File> files) throws ScmException {
        Commandline cl = GitCommandLineUtils.getBaseGitCommandLine(workingDirectory, "add");

        // use this separator to make clear that the following parameters are files and not revision info.
        cl.createArg().setValue("--");

        ScmSyncGitUtils.addTarget(cl, files);

        return cl;
    }

    protected AddScmResult executeAddFileSet(ScmFileSet fileSet) throws ScmException {
        File workingDirectory = fileSet.getBasedir();
        List<File> files = fileSet.getFileList();

        // command line can be too long for windows so add files individually (see SCM-697)
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            for (File file : files) {
                AddScmResult result = executeAddFiles(workingDirectory, Collections.singletonList(file));
                if (result != null) {
                    return result;
                }
            }
        } else {
            AddScmResult result = executeAddFiles(workingDirectory, files);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private AddScmResult executeAddFiles(File workingDirectory, List<File> files) throws ScmException {
        Commandline cl = createCommandLine(workingDirectory, files);

        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();

        int exitCode = -1;
        try {
            exitCode = GitCommandLineUtils.execute(cl, stdout, stderr, getLogger());
        } catch (Throwable t) {
            getLogger().error("Failed:", t);
        }
        if (exitCode != 0) {
            String msg = stderr.getOutput();
            getLogger().info("Add failed:" + msg);
            return new AddScmResult(cl.toString(), "The git-add command failed.", msg, false);
        }

        return null;
    }

}
