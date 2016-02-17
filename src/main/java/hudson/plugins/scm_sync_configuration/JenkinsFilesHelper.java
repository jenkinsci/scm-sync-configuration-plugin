package hudson.plugins.scm_sync_configuration;

import java.io.File;

import jenkins.model.Jenkins;

public class JenkinsFilesHelper {

    public static String buildPathRelativeToHudsonRoot(File file) {
        File jenkinsRoot = Jenkins.getInstance().getRootDir();
        String jenkinsRootPath = jenkinsRoot.getAbsolutePath();
        String fileAbsolutePath = file.getAbsolutePath();
        if (fileAbsolutePath.equals(jenkinsRootPath)) {
            // Hmmm. Should never occur.
            throw new IllegalArgumentException("Cannot build relative path to $JENKINS_HOME for $JENKINS_HOME itself; would be empty.");
        }
        if (!jenkinsRootPath.endsWith(File.separator)) {
            jenkinsRootPath += File.separator;
        }
        if (!fileAbsolutePath.startsWith(jenkinsRootPath)) {
            // Oops, the file is not relative to $JENKINS_HOME
            return null;
        }
        String truncatedPath = fileAbsolutePath.substring(jenkinsRootPath.length());
        return truncatedPath.replace(File.separatorChar, '/');
    }

    public static File buildFileFromPathRelativeToHudsonRoot(String pathRelativeToJenkinsRoot){
        return new File(Jenkins.getInstance().getRootDir(), pathRelativeToJenkinsRoot);
    }
}
