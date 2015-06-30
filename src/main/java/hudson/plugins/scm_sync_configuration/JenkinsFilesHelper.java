package hudson.plugins.scm_sync_configuration;

import java.io.File;

import jenkins.model.Jenkins;

public class JenkinsFilesHelper {

	public static String buildPathRelativeToHudsonRoot(File file){
		File jenkinsRoot = Jenkins.getInstance().getRootDir();
		if(!file.getAbsolutePath().startsWith(jenkinsRoot.getAbsolutePath())){
			return null;
		}
		String truncatedPath = file.getAbsolutePath().substring(jenkinsRoot.getAbsolutePath().length()+1); // "+1" because we don't need ending file separator
		return truncatedPath.replaceAll("\\\\", "/"); 
	}

    public static File buildFileFromPathRelativeToHudsonRoot(String pathRelativeToJenkinsRoot){
        File jenkinsRoot = Jenkins.getInstance().getRootDir();
        return new File(jenkinsRoot.getAbsolutePath(), pathRelativeToJenkinsRoot);
    }
}
