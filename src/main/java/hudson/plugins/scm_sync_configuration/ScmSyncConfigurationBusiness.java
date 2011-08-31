package hudson.plugins.scm_sync_configuration;

import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.FileUtils;

public class ScmSyncConfigurationBusiness {

	private static final String WORKING_DIRECTORY_PATH = "/scm-sync-configuration/";
	private static final String CHECKOUT_SCM_DIRECTORY = "checkoutConfiguration";
	private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationBusiness.class.getName());
	
	private boolean checkoutSucceeded;
	private SCMManipulator scmManipulator;
	private File checkoutScmDirectory = null;
	
	public ScmSyncConfigurationBusiness() {
	}
	
	public void init(ScmContext scmContext) throws ComponentLookupException, PlexusContainerException {
		ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
		this.scmManipulator = new SCMManipulator(scmManager);
		this.checkoutScmDirectory = new File(getCheckoutScmDirectoryAbsolutePath());
		this.checkoutSucceeded = false;
		initializeRepository(scmContext, false);
	}
	
	public void initializeRepository(ScmContext scmContext, boolean deleteCheckoutScmDir) {
		// Let's check if everything is available to checkout sources
		if (scmManipulator != null && scmManipulator.scmConfigurationSettledUp(scmContext, true)) {
			LOGGER.info("Initializing SCM repository for scm-sync-configuration plugin ...");
			// If checkoutScmDirectory was not empty and deleteCheckoutScmDir is asked, reinitialize it !
			if (deleteCheckoutScmDir) {
				cleanChekoutScmDirectory();
			}
			
			// Creating checkout scm directory
			if (!checkoutScmDirectory.exists()) {
				try {
					FileUtils.forceMkdir(checkoutScmDirectory);
					LOGGER.info("Directory [" + checkoutScmDirectory.getAbsolutePath() + "] created !");
				} catch (IOException e) {
					LOGGER.warning("Directory [" + checkoutScmDirectory.getAbsolutePath() + "] cannot be created !");
				}
			}
			
			this.checkoutSucceeded = this.scmManipulator.checkout(this.checkoutScmDirectory);
			if (this.checkoutSucceeded) {
				LOGGER.info("SCM repository initialization done.");
			}
		}
	}
	
	public void cleanChekoutScmDirectory() {
		if (checkoutScmDirectory.exists()) {
			LOGGER.info("Deleting old checkout SCM directory ...");
			try {
				FileUtils.forceDelete(checkoutScmDirectory);
			} catch (IOException e) {
				LOGGER.throwing(FileUtils.class.getName(), "forceDelete", e);
				LOGGER.severe("Error while deleting [" + checkoutScmDirectory.getAbsolutePath() + "] : " + e.getMessage());
			}
			this.checkoutSucceeded = false;
		}
	}
	
	public void deleteHierarchy(ScmContext scmContext, File rootHierarchy, User user) {
		deleteHierarchy(scmContext, rootHierarchy, createCommitMessage(
				scmContext.getScmCommentPrefix(),
				"Hierarchy deleted",
				user,
				null,
				scmContext.getScmCommentSuffix()));
	}
	
	public void deleteHierarchy(ScmContext scmContext, File rootHierarchy, String commitMessage) {
		if (scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false)) {
			return;
		}

		String rootHierarchyPathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(rootHierarchy);
		File rootHierarchyTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath() + File.separator + rootHierarchyPathRelativeToHudsonRoot);
	
		scmManipulator.deleteHierarchy(rootHierarchyTranslatedInScm, commitMessage);
	}
	
	public void renameHierarchy(ScmContext scmContext, File oldDir, File newDir, User user) {
		if (scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false)) {
			return;
		}
	
		String oldDirPathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(oldDir);
		String newDirPathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(newDir);
		File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());
		String commitMessage = createCommitMessage(
				scmContext.getScmCommentPrefix(),
				"Moved " + oldDirPathRelativeToHudsonRoot + " hierarchy to " + newDirPathRelativeToHudsonRoot,
				user,
				null,
				scmContext.getScmCommentSuffix());
		LOGGER.info("Renaming hierarchy [" + oldDirPathRelativeToHudsonRoot + "] to [" + newDirPathRelativeToHudsonRoot + "]");
		
		this.scmManipulator.renameHierarchy(scmRoot, oldDirPathRelativeToHudsonRoot, newDirPathRelativeToHudsonRoot, commitMessage);
	}
	
	public void synchronizeFile(ScmContext scmContext, File modifiedFile, String comment, User user) {
		if (scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false)) {
			return;
		}
	
		String modifiedFilePathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(modifiedFile);
		LOGGER.info("Synchronizing file [" + modifiedFilePathRelativeToHudsonRoot + "] to SCM ...");
		String commitMessage = createCommitMessage(
				scmContext.getScmCommentPrefix(),
				"Modification on file",
				user,
				comment,
				scmContext.getScmCommentSuffix());
		
		File modifiedFileTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath() + File.separator + modifiedFilePathRelativeToHudsonRoot);
		boolean modifiedFileAlreadySynchronized = modifiedFileTranslatedInScm.exists();
		try {
			FileUtils.copyFile(modifiedFile, modifiedFileTranslatedInScm);
		} catch (IOException e) {
			LOGGER.throwing(FileUtils.class.getName(), "copyFile", e);
			LOGGER.severe("Error while copying file : " + e.getMessage());
			return;
		}

		File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());
		
		List<File> synchronizedFiles = new ArrayList<File>();
		// if modified file is not yet synchronized with scm, let's add it !
		if (!modifiedFileAlreadySynchronized) {
			synchronizedFiles.addAll(this.scmManipulator.addFile(scmRoot, modifiedFilePathRelativeToHudsonRoot));
		} else {
			synchronizedFiles.add(new File(modifiedFilePathRelativeToHudsonRoot));
		}

		if (this.scmManipulator.checkinFiles(scmRoot, synchronizedFiles, commitMessage)) {
			LOGGER.info("Synchronized file [" + modifiedFilePathRelativeToHudsonRoot + "] to SCM !");
		}
	}
	
	public void synchronizeAllConfigs(ScmContext scmContext, ScmSyncStrategy[] availableStrategies, User user) {
		List<File> filesToSync = new ArrayList<File>();
		// Building synced files from strategies
		for (ScmSyncStrategy strategy : availableStrategies) {
			filesToSync.addAll(strategy.createInitializationSynchronizedFileset());
		}
		
		for (File fileToSync : filesToSync) {
			String hudsonConfigPathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(fileToSync);
			File hudsonConfigTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath() + File.separator + hudsonConfigPathRelativeToHudsonRoot);
			try {
				if (!hudsonConfigTranslatedInScm.exists()
						|| !FileUtils.contentEquals(hudsonConfigTranslatedInScm, fileToSync)) {
					synchronizeFile(scmContext, fileToSync, "Synchronization init", user);
				}
			} catch (IOException e) {
			}
		}
	}

	public boolean scmCheckoutDirectorySettledUp(ScmContext scmContext) {
		return scmManipulator != null && this.scmManipulator.scmConfigurationSettledUp(scmContext, false) && this.checkoutSucceeded;
	}
	
	private static String createCommitMessage(String messagePrefix, String message, User user, String comment, String messageSuffix) {
		StringBuilder commitMessage = new StringBuilder();
		commitMessage.append(messagePrefix).append(" ");
		commitMessage.append(message);
		if (user != null) {
			commitMessage.append(" by ").append(user.getId());
		}
		if (comment != null) {
			commitMessage.append(" with following comment : ").append(comment);
		}
		commitMessage.append(" ").append(messageSuffix);
		return commitMessage.toString();
	}
	
	private static String getCheckoutScmDirectoryAbsolutePath() {
		return Hudson.getInstance().getRootDir().getAbsolutePath() + WORKING_DIRECTORY_PATH + CHECKOUT_SCM_DIRECTORY;
	}
}
