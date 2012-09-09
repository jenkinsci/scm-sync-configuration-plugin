package hudson.plugins.scm_sync_configuration.model;

import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationBusiness;

import java.io.File;

/**
 * @author fcamblor
 * Paths allows to know if a given path is a directory or not, without using a File object since,
 * generally, Path will be relative to jenkins root
 */
public class Path {

    private String path;
    private boolean isDirectory;

    public Path(String path){
        this(JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(path));
    }

    public Path(File hudsonFile){
        this(JenkinsFilesHelper.buildPathRelativeToHudsonRoot(hudsonFile), hudsonFile.isDirectory());
    }

    public Path(String path, boolean isDirectory) {
        this.path = path;
        this.isDirectory = isDirectory;
    }

    public String getPath() {
        return path;
    }

    public File getHudsonFile(){
        return JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(this.path);
    }

    public File getScmFile(){
        // TODO: Externalize ScmSyncConfigurationBusiness.getCheckoutScmDirectoryAbsolutePath()
        // in another class ?
        return new File(ScmSyncConfigurationBusiness.getCheckoutScmDirectoryAbsolutePath()+File.separator+getPath());
    }

    public String getFirstNonExistingParentScmPath(){
        File scmFile = getScmFile();
        File latestNonExistingScmFile = null;
        File currentFile = scmFile;
        while(!currentFile.exists()){
            latestNonExistingScmFile = currentFile;
            currentFile = currentFile.getParentFile();
        }

        return latestNonExistingScmFile.getAbsolutePath().substring(ScmSyncConfigurationBusiness.getCheckoutScmDirectoryAbsolutePath().length()+1);
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean contains(Path p){
        return this.isDirectory() && p.getPath().startsWith(this.getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Path)) return false;

        Path path1 = (Path) o;

        if (isDirectory != path1.isDirectory) return false;
        if (path != null ? !path.equals(path1.path) : path1.path != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (isDirectory ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return getPath()+(isDirectory()?"/":"");
    }
}
