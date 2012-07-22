package hudson.plugins.scm_sync_configuration.strategies.model;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import org.apache.tools.ant.DirectoryScanner;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PatternsEntityMatcher implements ConfigurationEntityMatcher {

    private static final Function<String,String> FILE_SEPARATOR_TRANSFORMER = new Function<String, String>() {
        public String apply(@Nullable String s) {
            return s.replaceAll("/", File.separator);
        }
    };

	private String[] patterns;

	public PatternsEntityMatcher(String[] patterns) {
		this.patterns = Collections2.transform(Arrays.asList(patterns), FILE_SEPARATOR_TRANSFORMER).toArray(new String[0]);
	}

	public boolean matches(Saveable saveable, File file) {
		if (file == null) {
			return false;
		}
		String filePathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file);
		for(String pattern : patterns) {
            if(DirectoryScanner.match(pattern, filePathRelativeToHudsonRoot)){
                return true;
            }
		}
		return false;
	}

    public List<String> getIncludes(){
        return Arrays.asList(patterns);
    }

    public String[] matchingFilesFrom(File rootDirectory) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(patterns);
        scanner.setBasedir(rootDirectory);
        scanner.scan();
        return scanner.getIncludedFiles();
    }
}
