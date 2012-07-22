package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

import java.io.File;
import java.util.List;

public interface ConfigurationEntityMatcher {
	public boolean matches(Saveable saveable, File file);
    public String[] matchingFilesFrom(File rootDirectory);
    List<String> getIncludes();
}
