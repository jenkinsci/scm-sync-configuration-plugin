package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.types.selectors.FileSelector;

public interface ConfigurationEntityMatcher {
	public boolean matches(Saveable saveable, File file);
	public boolean matches(Saveable saveable, String pathRelativeToRoot, boolean isDirectory);
    public String[] matchingFilesFrom(File rootDirectory, FileSelector selector);
    List<String> getIncludes();
}
