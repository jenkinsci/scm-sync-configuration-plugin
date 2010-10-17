package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

public class ClassOnlyConfigurationEntityMatcher extends ClassAndFileConfigurationEntityMatcher {

	public ClassOnlyConfigurationEntityMatcher(Class<? extends Saveable> clazz){
		super(clazz, ".*");
	}
}
