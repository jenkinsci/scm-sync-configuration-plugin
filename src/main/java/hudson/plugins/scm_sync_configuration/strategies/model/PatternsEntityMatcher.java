package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import org.apache.tools.ant.DirectoryScanner;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PatternsEntityMatcher implements ConfigurationEntityMatcher {

	private String[] includesPatterns;

    public PatternsEntityMatcher(String[] includesPatterns){
        this.includesPatterns = includesPatterns;
    }

	public boolean matches(Saveable saveable, File file) {
		if (file == null) {
			return false;
		}
		String filePathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file);
		AntPathMatcher matcher = new AntPathMatcher();
		for (String pattern : includesPatterns) {
            if (matcher.match(pattern, filePathRelativeToHudsonRoot)) {
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
        scanner.setIncludes(includesPatterns);
        scanner.setBasedir(rootDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }
}
