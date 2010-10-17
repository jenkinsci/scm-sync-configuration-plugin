package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

import java.io.File;

public interface ConfigurationEntityMatcher {

	public boolean matches(Saveable saveable, File file);
}
