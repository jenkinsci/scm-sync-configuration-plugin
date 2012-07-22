package hudson.plugins.scm_sync_configuration.strategies;

import java.io.File;
import java.util.Collection;
import java.util.List;

import hudson.model.Saveable;

public interface ScmSyncStrategy {

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
}
