package hudson.plugins.scm_sync_configuration.strategies;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;

import java.io.File;
import java.util.List;

public interface ScmSyncStrategy {

    public static interface CommitMessageFactory {
        public WeightedMessage getMessageWhenSaveableUpdated(Saveable s, XmlFile file);
        public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath);
        public WeightedMessage getMessageWhenItemDeleted(Item item);
    }

	/**
	 * Is the given Saveable eligible for the current strategy ?
	 * @param saveable A saveable which is saved
	 * @param file Corresponding file to the given Saveable object
	 * @return true if current Saveable instance matches with current ScmSyncStrategy target,
	 * false otherwise
	 */
	boolean isSaveableApplicable(Saveable saveable, File file);
	
	/**
	 * Determines whether the strategy might have applied to a deleted item.
	 * 
	 * @param saveable that was deleted; still exists in Jenkins' model but has already been eradicated from disk
	 * @param pathRelativeToRoot where the item resided
	 * @param wasDirectory whether it was a directory
	 * @return 
	 */
	boolean mightHaveBeenApplicableToDeletedSaveable(Saveable saveable, String pathRelativeToRoot, boolean wasDirectory);
	
	/**
	 * Is the given url eligible for the current strategy ?
	 * @param url Current url, where hudson root url has been truncated
	 * @return true if current url matches with current ScmSyncStrategy target, false otherwise
	 */
	boolean isCurrentUrlApplicable(String url);
	
	/**
	 * Collects all files, from Jenkins' root directory, that match this strategy.
	 * 
	 * @return the list of files matched.
	 */
	List<File> collect();

	/**
	 * Collects all files in the given directory, which must be under Jenkins' root directory, that match this strategy.
	 * 
	 * @param directory to search in
	 * @return the list of files
	 * @throws IllegalArgumentException if the given directory is not under Jenkins' root directory
	 */
	List<File> collect(File directory);
	
    /**
     * @return List of sync'ed file includes brought by current strategy. Used only for informational purposes in the UI.
     */
    List<String> getSyncIncludes();

    /**
     * @return A Factory intended to generate commit message depending on contexts
     */
    CommitMessageFactory getCommitMessageFactory();
}
