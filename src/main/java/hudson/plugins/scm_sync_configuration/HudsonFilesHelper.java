package hudson.plugins.scm_sync_configuration;

import hudson.model.Hudson;

import java.io.File;

public class HudsonFilesHelper {

	public static String buildPathRelativeToHudsonRoot(File filePath){
		File hudsonRoot = Hudson.getInstance().getRootDir();
		if(!filePath.getAbsolutePath().startsWith(hudsonRoot.getAbsolutePath())){
			throw new IllegalArgumentException("Err ! File <"+filePath.getAbsolutePath()+"> seems not to reside in <"+hudsonRoot.getAbsolutePath()+"> !");
		}
		return filePath.getAbsolutePath().substring(hudsonRoot.getAbsolutePath().length()+1); // "+1" because we don't need ending file separator
	}
}
