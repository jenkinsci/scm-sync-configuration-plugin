package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

import java.io.File;
import java.util.regex.Pattern;

public class ClassAndFileConfigurationEntityMatcher extends PatternsEntityMatcher {
		
	private Class<? extends Saveable> saveableClazz;
	
	public ClassAndFileConfigurationEntityMatcher(Class<? extends Saveable> clazz, String[] patterns){
		super(patterns);
		this.saveableClazz = clazz;
	}
	
	public boolean matches(Saveable saveable, File file) {
		if(saveableClazz.isAssignableFrom(saveable.getClass())){
			if(file == null){
				return true;
			} else {
				return super.matches(saveable, file);
			}
		}
		
		return false;
	}

}
