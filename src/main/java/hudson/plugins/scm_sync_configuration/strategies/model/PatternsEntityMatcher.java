package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;

import java.io.File;
import java.util.regex.Pattern;

public class PatternsEntityMatcher implements ConfigurationEntityMatcher {

	private Pattern [] patterns;

	public PatternsEntityMatcher(Pattern [] patterns) {
		this.patterns = patterns;
	}

	public boolean matches(Saveable saveable, File file) {
		if (file == null) {
			return false;
		}
		String filePathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file);
		for(Pattern pattern : patterns) {
			if (pattern.matcher(filePathRelativeToHudsonRoot).matches()) {
				return true;
			}
		}
		return false;
	}

}
