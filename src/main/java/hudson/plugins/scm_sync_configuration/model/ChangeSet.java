package hudson.plugins.scm_sync_configuration.model;

import com.google.common.io.Files;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.exceptions.LoggableException;
import hudson.plugins.scm_sync_configuration.utils.Checksums;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author fcamblor
 * POJO representing a Changeset built during a scm transaction
 */
public class ChangeSet {

    // Changeset commit message
    WeightedMessage message = null;
    // [Path, content in bytes] which are queued for addition/modification
    Map<Path, byte[]> pathContents;
    // Paths which are queued for deletion
    List<Path> pathsToDelete;

    public ChangeSet(){
        pathContents = new HashMap<Path, byte[]>();
        pathsToDelete = new ArrayList<Path>();
    }

    public void registerPath(String path) {
        boolean contentAlreadyRegistered = false;
        File hudsonFile = JenkinsFilesHelper.buildFileFromPathRelativeToHudsonRoot(path);
        Path pathToRegister = new Path(hudsonFile);

        if(pathToRegister.isDirectory()){
            pathContents.put(pathToRegister, new byte[0]);
        } else {
            // Verifying if path content is already in pathContent and, if this is the case,
            // look at checksums
            if(pathContents.containsKey(pathToRegister)){
                try {
                    contentAlreadyRegistered = Checksums.fileAndByteArrayContentAreEqual(pathToRegister.getHudsonFile(), pathContents.get(pathToRegister));
                } catch (IOException e) {
                    throw new LoggableException("Changeset path <"+path+"> registration failed", Checksums.class, "fileAndByteArrayContentAreEqual", e);
                }
            }

            if(!contentAlreadyRegistered){
                try {
                    pathContents.put(pathToRegister, Files.toByteArray(pathToRegister.getHudsonFile()));
                } catch (IOException e) {
                    throw new LoggableException("Changeset path <"+path+"> registration failed", Files.class, "toByteArray", e);
                }
            }
        }
    }

    public void registerPathForDeletion(String path){
        // We should determine if path is a directory by watching scm path (and not hudson path) because in most of time,
        // when we are here, directory is already deleted in hudson hierarchy...
        boolean isDirectory = new Path(path).getScmFile().isDirectory();
        pathsToDelete.add(new Path(path, isDirectory));
    }

    public boolean isEmpty(){
        return pathContents.isEmpty() && pathsToDelete.isEmpty();
    }

    public Map<Path, byte[]> getPathContents(){
        Map<Path, byte[]> filteredPathContents = new HashMap<Path, byte[]>(pathContents);

        // Avoiding ConcurrentModificationException...
        List<Path> filteredPaths = new ArrayList<Path>();

        for(Path pathToAdd : filteredPathContents.keySet()){
            for(Path pathToDelete : pathsToDelete){
                // Removing paths being both in pathsToDelete and pathContents
                if(pathToDelete.contains(pathToAdd)){
                    filteredPaths.add(pathToAdd);
                }
            }
        }

        for(Path path : filteredPaths){
            filteredPathContents.remove(path);
        }

        return filteredPathContents;
    }

    public List<Path> getPathsToDelete(){
        return Collections.unmodifiableList(pathsToDelete);
    }

    public void defineMessage(WeightedMessage weightedMessage) {
        // Defining message only once !
        if(this.message == null || weightedMessage.getWeight().compareTo(message.getWeight()) > 0){
            this.message = weightedMessage;
        }
    }

    public String getMessage(){
        return this.message.getMessage();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Path path : getPathContents().keySet()){
            sb.append(String.format("    A %s%n", path.toString()));
        }
        for(Path path : getPathsToDelete()){
            sb.append(String.format("    D %s%n", path.toString()));
        }
        return sb.toString();
    }
}
