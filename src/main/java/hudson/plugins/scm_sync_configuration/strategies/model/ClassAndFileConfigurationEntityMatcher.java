package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;

import java.io.File;
import java.util.regex.Pattern;

public class ClassAndFileConfigurationEntityMatcher implements
		ConfigurationEntityMatcher {

	private Class<? extends Saveable> saveableClazz;
	private Pattern filePathRegex;

	public ClassAndFileConfigurationEntityMatcher(Class<? extends Saveable> clazz, String _filePathRegex){
		this.saveableClazz = clazz;
		this.filePathRegex = Pattern.compile(_filePathRegex);
	}
	
	public boolean matches(Saveable saveable, File file) {
		if(saveableClazz.isAssignableFrom(saveable.getClass())){
			if(file == null){
				return true;
			} else {
				String filePathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file);
				return this.filePathRegex.matcher(filePathRelativeToHudsonRoot).matches();
			}
		}
		
		return false;
	}

}
