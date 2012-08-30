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
	 * Is the given url eligible for the current strategy ?
	 * @param url Current url, where hudson root url has been truncated
	 * @return true if current url matches with current ScmSyncStrategy target, false otherwise
	 */
	boolean isCurrentUrlApplicable(String url);
	
	/**
	 * @return a Fileset of file to synchronize when initializing scm repository
	 */
	List<File> createInitializationSynchronizedFileset();

    /**
     * @return List of sync'ed file includes brought by current strategy
     */
    List<String> getSyncIncludes();

    /**
     * @return A Factory intended to generate commit message depending on contexts
     */
    CommitMessageFactory getCommitMessageFactory();
}
