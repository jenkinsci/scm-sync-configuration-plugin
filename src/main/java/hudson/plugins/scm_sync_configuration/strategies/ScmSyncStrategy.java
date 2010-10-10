package hudson.plugins.scm_sync_configuration.strategies;

import hudson.model.Saveable;

public interface ScmSyncStrategy {

	/**
	 * Is the given Saveable eligible for the current strategy ?
	 * @param saveable A saveable which is saved
	 * @return true if current Saveable instance matches with current ScmSyncStrategy target,
	 * false otherwise
	 */
	boolean isSaveableApplicable(Saveable saveable);
	
	/**
	 * Is the given url eligible for the current strategy ?
	 * @param url Current url, where hudson root url has been truncated
	 * @return true if current url matches with current ScmSyncStrategy target, false otherwise
	 */
	boolean isCurrentUrlApplicable(String url);
}
