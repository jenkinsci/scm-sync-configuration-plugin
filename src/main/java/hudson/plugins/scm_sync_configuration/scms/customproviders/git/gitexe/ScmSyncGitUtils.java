package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.git.gitexe.command.GitCommandLineUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

import com.google.common.base.Ascii;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

public final class ScmSyncGitUtils {

	private ScmSyncGitUtils() {
		// No instantiation
	}
	
	public static File getRepositoryRootDirectory(File someDirectoryInTheRepo, ScmLogger logger) throws ScmException {
		// Factored out from GitStatusCommand.
        Commandline cl = GitCommandLineUtils.getBaseGitCommandLine(someDirectoryInTheRepo, "rev-parse");
        cl.createArg().setValue("--show-toplevel");
        
    	CommandLineUtils.StringStreamConsumer stdout = new CommandLineUtils.StringStreamConsumer();
        CommandLineUtils.StringStreamConsumer stderr = new CommandLineUtils.StringStreamConsumer();

        int exitCode = GitCommandLineUtils.execute(cl, stdout, stderr, logger);
        
        if (exitCode != 0) {
            // git-status returns non-zero if nothing to do
            if (logger.isInfoEnabled()) {
                logger.info( "Could not resolve toplevel from " + someDirectoryInTheRepo);
            }
        } else {
        	String absoluteRepositoryRoot = stdout.getOutput().trim(); // This comes back unquoted!
        	if (!Strings.isNullOrEmpty(absoluteRepositoryRoot)) {
        		return new File(absoluteRepositoryRoot);
        	}
        }
        return null;
	}

	// Fix for https://issues.apache.org/jira/browse/SCM-695
    public static void addTarget(Commandline cl, List<File> files) throws ScmException {
    	// Fix for https://issues.apache.org/jira/browse/SCM-695 . Function copied from
    	// GitCommandLineUtils.
        if (files == null || files.isEmpty()) {
            return;
        }
        final File workingDirectory = cl.getWorkingDirectory();
        try
        {
            String canonicalWorkingDirectory = workingDirectory.getCanonicalPath();
            if (!canonicalWorkingDirectory.endsWith(File.separator)) {
            	canonicalWorkingDirectory += File.separator; // Fixes SCM-695
            }
            for (File file : files) {
                String relativeFile = file.getPath();
                final String canonicalFile = file.getCanonicalPath();
                
                if (canonicalFile.startsWith(canonicalWorkingDirectory)) {
                    relativeFile = canonicalFile.substring(canonicalWorkingDirectory.length());
                    if (relativeFile.startsWith(File.separator)) {
                        relativeFile = relativeFile.substring(File.separator.length());
                    }
                }
                // no setFile() since this screws up the working directory!
                cl.createArg().setValue( relativeFile );
            }
        }
        catch ( IOException ex )
        {
            throw new ScmException( "Could not get canonical paths for workingDirectory = "
                + workingDirectory + " or files=" + files, ex );
        }
    }

    public static String relativizePath (String parent, String child) {
        // Fix for SCM-772. Compare FixedGitStatusConsumer.
        if ( parent != null && child != null) {
        	if (parent.equals(child)) {
        		return "";
        	}
        	if (!parent.endsWith(File.separator)) {
        		parent += File.separator;
        	}
        	if (child.startsWith(parent)) {
        		child = child.substring(parent.length());
        		if (child.startsWith(File.separator)) {
        			child = child.substring(File.separator.length());
        		}
        	}
        }
        return child;
    }

    public static String dequote(String inputFromGit) {
    	if (inputFromGit.charAt(0) != '"') {
    		return inputFromGit;
    	}
    	// Per http://git-scm.com/docs/git-status : If a filename contains whitespace or other nonprintable characters,
    	// that field will be quoted in the manner of a C string literal: surrounded by ASCII double quote (34) characters,
    	// and with interior special characters backslash-escaped.
    	//
    	// Here, we have to undo this. We work on byte sequences and assume UTF-8. (Git may also give us back non-ASCII
    	// characters back as UTF-8 byte sequences that appear as octal or hex escapes.)
    	byte[] input = inputFromGit.substring(1, inputFromGit.length() - 1).getBytes(Charsets.UTF_8);
    	byte[] output = new byte[input.length]; // It can only get smaller
    	int j = 0;
    	for (int i = 0; i < input.length; i++, j++) {
    		if (input[i] == '\\') {
    			byte ch = input[++i];
    			switch (ch) {
    			case '\\' :
    			case '"' :
    				output[j] = ch;
    				break;
    			case 'a' :
    				output[j] = Ascii.BEL;
    				break;
    			case 'b' :
    				output[j] = '\b';
    				break;
    			case 'f' :
    				output[j] = '\f';
    				break;
    			case 'n' :
    				output[j] = '\n';
    				break;
    			case 'r' :
    				output[j] = '\r';
    				break;
    			case 't' :
    				output[j] = '\t';
    				break;
    			case 'v' :
    				output[j] = Ascii.VT;
    				break;
    			case 'x' :
    				// Hex escape; must be followed by two hex digits. We assume git gives us only valid sequences.
    				if (i + 2 < input.length) {
    					byte value = toHex(input[++i]);
    					output[j] = (byte) (value * 16 + toHex(input[++i]));
    				} else {
    					// Invalid.
        				output[j++] = '\\';
        				output[j] = ch;
    				}
    				break;
    			case '0' :
    			case '1' :
    			case '2' :
    			case '3' :
    				// Octal escape; must be followed by two more octal digits. We assume git gives us only valid sequences.
    				if (i + 2 < input.length) {
    					byte value = (byte) (ch - '0');
    					value = (byte) (value * 8 + (byte) (input[++i] - '0'));
    					output[j] = (byte) (value * 8 + (byte) (input[++i] - '0'));
    				} else {
    					// Invalid.
        				output[j++] = '\\';
        				output[j] = ch;
    				}
    				break;
    			default :
    				// Unknown/invalid escape.
    				output[j++] = '\\';
    				output[j] = ch;
    				break;
    			}
    		} else {
    			output[j] = input[i];
    		}
    	}
    	return new String(output, 0, j, Charsets.UTF_8);
    }

    private static byte toHex(byte in) {
    	if (in >= '0' && in <= '9') {
    		return (byte) (in - '0');
    	} else if (in >= 'A' && in <= 'F') {
    		return (byte) (in - 'A');
    	} else if (in >= 'a' && in <= 'f') {
    		return (byte) (in - 'a');
    	}
    	return in;
    }

}
