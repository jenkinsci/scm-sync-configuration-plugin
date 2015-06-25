package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;

import org.apache.tools.ant.DirectoryScanner;
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
		String pathRelativeToRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file);
		// Guard our own SCM workspace and the war directory. User-defined includes might inadvertently include those if they start with * or **!
		if (pathRelativeToRoot.equals(SCM_WORKING_DIRECTORY) || pathRelativeToRoot.startsWith(SCM_WORKING_DIRECTORY + '/')) {
			return false;
		} else if (pathRelativeToRoot.equals(WAR_DIRECTORY) || pathRelativeToRoot.startsWith(WAR_DIRECTORY + '/')) {
			return false;
		}
        AntPathMatcher matcher = new AntPathMatcher();
        for (String pattern : includesPatterns) {
            if (matcher.match(pattern, pathRelativeToRoot)) {
                return true;
            }
		}
		return false;
	}

    public List<String> getIncludes(){
        return Arrays.asList(includesPatterns);
    }

    public String[] matchingFilesFrom(File rootDirectory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setExcludes(new String[] { SCM_WORKING_DIRECTORY, SCM_WORKING_DIRECTORY + '/', WAR_DIRECTORY, WAR_DIRECTORY + '/'}); // Guard special directories
        scanner.setIncludes(includesPatterns);
        scanner.setBasedir(rootDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }
    
}
