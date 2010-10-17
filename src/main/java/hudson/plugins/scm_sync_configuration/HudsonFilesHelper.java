package hudson.plugins.scm_sync_configuration;

import hudson.model.Hudson;

import java.io.File;

public class HudsonFilesHelper {

	public static String buildPathRelativeToHudsonRoot(File file){
		File hudsonRoot = Hudson.getInstance().getRootDir();
		if(!file.getAbsolutePath().startsWith(hudsonRoot.getAbsolutePath())){
			throw new IllegalArgumentException("Err ! File <"+file.getAbsolutePath()+"> seems not to reside in <"+hudsonRoot.getAbsolutePath()+"> !");
		}
		String truncatedPath = file.getAbsolutePath().substring(hudsonRoot.getAbsolutePath().length()+1); // "+1" because we don't need ending file separator
		return truncatedPath.replaceAll("\\\\", "/"); 
	}
}
