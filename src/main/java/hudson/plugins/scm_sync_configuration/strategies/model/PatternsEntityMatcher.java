package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PatternsEntityMatcher implements ConfigurationEntityMatcher {

	private String[] includesPatterns;

	private static String SCM_WORKING_DIRECTORY = ScmSyncConfigurationBusiness.getScmDirectoryName();
	private static String WAR_DIRECTORY = "war";
	
    public PatternsEntityMatcher(String[] includesPatterns){
        this.includesPatterns = includesPatterns;
    }

	public boolean matches(Saveable saveable, File file) {
		if (file == null) {
			return false;
		}
		return matches(saveable, JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file), file.isDirectory());
	}

	public boolean matches(Saveable saveable, String pathRelativeToRoot, boolean isDirectory) {
		if (pathRelativeToRoot != null) {
			// Guard our own SCM workspace and the war directory. User-defined includes might inadvertently include those if they start with * or **!
			if (pathRelativeToRoot.equals(SCM_WORKING_DIRECTORY) || pathRelativeToRoot.startsWith(SCM_WORKING_DIRECTORY + '/')) {
				return false;
			} else if (pathRelativeToRoot.equals(WAR_DIRECTORY) || pathRelativeToRoot.startsWith(WAR_DIRECTORY + '/')) {
				return false;
			}
	        AntPathMatcher matcher = new AntPathMatcher();
	        String directoryName = null;
	        for (String pattern : includesPatterns) {
	            if (matcher.match(pattern, pathRelativeToRoot)) {
	                return true;
	            } else if (isDirectory) {
	            	// pathRelativeFromRoot is be a directory, and the pattern end in a file name. In this case, we must claim a match.
	            	int i = pattern.lastIndexOf('/');
	            	if (directoryName == null) {
	            		directoryName = pathRelativeToRoot.endsWith("/") ? pathRelativeToRoot.substring(0, pathRelativeToRoot.length() - 1) : pathRelativeToRoot;
	            	}
	            	if (i > 0 && matcher.match(pattern.substring(0, i), directoryName)) {
	            		return true;
	            	}
	            }
			}
		}
		return false;
	}

    public List<String> getIncludes(){
        return Arrays.asList(includesPatterns);
    }
    
    public String[] matchingFilesFrom(File rootDirectory, FileSelector selector) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setExcludes(new String[] { SCM_WORKING_DIRECTORY, SCM_WORKING_DIRECTORY + '/', WAR_DIRECTORY, WAR_DIRECTORY + '/'}); // Guard special directories
        scanner.setIncludes(includesPatterns);
        scanner.setBasedir(rootDirectory);
        if (selector != null) {
        	scanner.setSelectors(new FileSelector[] { selector});
        }
        scanner.scan();
        return scanner.getIncludedFiles();
    }

}
