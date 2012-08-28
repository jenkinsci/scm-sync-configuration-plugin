package hudson.plugins.scm_sync_configuration;

import com.google.common.io.Files;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.exceptions.LoggableException;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.model.Commit;
import hudson.plugins.scm_sync_configuration.model.Path;
import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.utils.Checksums;
import hudson.util.DaemonThreadFactory;
import org.apache.commons.io.FileUtils;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class ScmSyncConfigurationBusiness {

	private static final String WORKING_DIRECTORY_PATH = "/scm-sync-configuration/";
	private static final String CHECKOUT_SCM_DIRECTORY = "checkoutConfiguration";
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationBusiness.class.getName());
	
    private boolean checkoutSucceeded;
	private SCMManipulator scmManipulator;
	private File checkoutScmDirectory = null;
	private ScmSyncConfigurationStatusManager scmSyncConfigurationStatusManager = null;

    /**
     * Use of a size 1 thread pool frees us from worrying about accidental thread death and
     * changeset commit concurrency
     */
    /*package*/ final ExecutorService writer = Executors.newFixedThreadPool(1, new DaemonThreadFactory());

    private List<Commit> commitsQueue = Collections.synchronizedList(new ArrayList<Commit>());

	public ScmSyncConfigurationBusiness(){
	}
	
	public ScmSyncConfigurationStatusManager getScmSyncConfigurationStatusManager() {
		if (scmSyncConfigurationStatusManager == null) {
			scmSyncConfigurationStatusManager = new ScmSyncConfigurationStatusManager();
		}
		return scmSyncConfigurationStatusManager;
	}

	public void init(ScmContext scmContext) throws ComponentLookupException, PlexusContainerException {
		ScmManager scmManager = SCMManagerFactory.getInstance().createScmManager();
		this.scmManipulator = new SCMManipulator(scmManager);
		this.checkoutScmDirectory = new File(getCheckoutScmDirectoryAbsolutePath());
		this.checkoutSucceeded = false;
		initializeRepository(scmContext, false);
	}
	
	public void initializeRepository(ScmContext scmContext, boolean deleteCheckoutScmDir){
		// Let's check if everything is available to checkout sources
		if(scmManipulator != null && scmManipulator.scmConfigurationSettledUp(scmContext, true)){
			LOGGER.info("Initializing SCM repository for scm-sync-configuration plugin ...");
			// If checkoutScmDirectory was not empty and deleteCheckoutScmDir is asked, reinitialize it !
			if(deleteCheckoutScmDir){
				cleanChekoutScmDirectory();
			}
			
			// Creating checkout scm directory
			if(!checkoutScmDirectory.exists()){
				try {
					FileUtils.forceMkdir(checkoutScmDirectory);
					LOGGER.info("Directory ["+ checkoutScmDirectory.getAbsolutePath() +"] created !");
				} catch (IOException e) {
					LOGGER.warning("Directory ["+ checkoutScmDirectory.getAbsolutePath() +"] cannot be created !");
				}
			}
			
			this.checkoutSucceeded = this.scmManipulator.checkout(this.checkoutScmDirectory);
			if(this.checkoutSucceeded){
				LOGGER.info("SCM repository initialization done.");
			}
			signal("Checkout " + this.checkoutScmDirectory, this.checkoutSucceeded);
		}
	}
	
	public void cleanChekoutScmDirectory(){
		if(checkoutScmDirectory != null && checkoutScmDirectory.exists()){
			LOGGER.info("Deleting old checkout SCM directory ...");
			try {
				FileUtils.forceDelete(checkoutScmDirectory);
			} catch (IOException e) {
				LOGGER.throwing(FileUtils.class.getName(), "forceDelete", e);
				LOGGER.severe("Error while deleting ["+checkoutScmDirectory.getAbsolutePath()+"] : "+e.getMessage());
			}
			this.checkoutSucceeded = false;
		}
	}
	
    public List<File> deleteHierarchy(ScmContext scmContext, String hierarchyPath){
        if(scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false)){
            return null;
        }

        File rootHierarchyTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+hierarchyPath);

        List<File> filesToCommit = scmManipulator.deleteHierarchy(rootHierarchyTranslatedInScm);
        signal("Delete " + hierarchyPath, filesToCommit != null);

        return filesToCommit;
    }

    public void queueChangeSet(final ScmContext scmContext, ChangeSet changeset, User user, String commitMessage) {
        // TODO: ensure commitMessage is the _complete_ message (and not just current session comment)
        // It should change compared to synchronizeFile() ...
        if(scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false)){
            LOGGER.info("Queue of changeset "+changeset.toString()+" aborted (scm manipulator not settled !)");
            return;
      	}

        Commit commit = new Commit(createCommitMessage(scmContext, changeset.getMessage(), user, commitMessage), changeset);
        LOGGER.info("Queuing commit "+commit.toString()+" to SCM ...");
        commitsQueue.add(commit);

        writer.execute(new Runnable() {
            public void run() {
                File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());

                // Copying shared commitQueue in order to allow conccurrent modification
                List<Commit> currentCommitQueue = new ArrayList<Commit>(commitsQueue);
                List<Commit> checkedInCommits = new ArrayList<Commit>();

                try {
                    // Reading commit queue and commiting changeset
                    for(Commit commit: currentCommitQueue){
                        String logMessage = "Processing commit "+commit.toString();
                        LOGGER.info(logMessage);

                        List<File> synchronizedFiles = new ArrayList<File>();
                        for(Map.Entry<Path,byte[]> pathContent : commit.getChangeset().getPathContents().entrySet()){
                            Path pathRelativeToJenkinsRoot = pathContent.getKey();
                            byte[] content = pathContent.getValue();

                            File fileTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+pathRelativeToJenkinsRoot.getPath());
                            boolean fileAlreadySynchronized = fileTranslatedInScm.exists();
                            if(!fileAlreadySynchronized){
                                Stack<File> directoriesToCreate = new Stack<File>();
                                File directory = fileTranslatedInScm.getParentFile();
                                while(!directory.exists()){
                                    directoriesToCreate.push(directory);
                                    directory = directory.getParentFile();
                                }
                                while(!directoriesToCreate.empty()){
                                    directory = directoriesToCreate.pop();
                                    if(!directory.mkdir()){
                                        throw new LoggableException("Error while creating directory "+directory.getAbsolutePath(), File.class, "mkdir");
                                    }
                                }
                                try {
                                    if(pathRelativeToJenkinsRoot.isDirectory()){
                                        if(!fileTranslatedInScm.mkdir()){
                                            throw new LoggableException("Error while creating directory "+fileTranslatedInScm.getAbsolutePath(), File.class, "mkdir");
                                        }
                                    } else {
                                        Files.write(content, fileTranslatedInScm);
                                    }
                                } catch (IOException e) {
                                    throw new LoggableException("Error while creating file in checkouted directory", Files.class, "write", e);
                                }
                                synchronizedFiles.addAll(scmManipulator.addFile(scmRoot, pathRelativeToJenkinsRoot.getPath()));
                            } else {
                                boolean contentAlreadyPresent = false;
                                try {
                                    contentAlreadyPresent = Checksums.fileAndByteArrayContentAreEqual(fileTranslatedInScm, content);
                                } catch (IOException e) {
                                    throw new LoggableException(logMessage, Checksums.class, "fileAndByteArrayContentAreEqual", e);
                                }
                                if(contentAlreadyPresent){
                                    // Don't do anything
                                } else {
                                    try {
                                        Files.write(content, fileTranslatedInScm);
                                    } catch (IOException e) {
                                        throw new LoggableException(logMessage, Files.class, "write", e);
                                    }
                                    synchronizedFiles.add(fileTranslatedInScm);
                                }
                            }
                        }

                        for(Path path : commit.getChangeset().getPathsToDelete()){
                            List<File> deletedFiles = deleteHierarchy(scmContext, path.getPath());
                            synchronizedFiles.addAll(deletedFiles);
                        }

                        if(synchronizedFiles.isEmpty()){
                            LOGGER.info("Empty changeset to commit (no changes found on files) => commit skipped !");
                        } else {
                            boolean result = scmManipulator.checkinFiles(scmRoot, synchronizedFiles, commit.getMessage());

                            if(result){
                                LOGGER.info("Commit "+commit.toString()+" pushed to SCM !");
                                checkedInCommits.add(commit);
                            } else {
                                throw new LoggableException(logMessage, SCMManipulator.class, "checkinFiles");
                            }

                            signal(logMessage, result);
                        }
                    }
                }catch(LoggableException e){
                    LOGGER.throwing(e.getClazz().getName(), e.getMethodName(), e);
                    LOGGER.severe("Error while copying file : "+e.getMessage());
                    signal(e.getMessage(), false);
                }

                commitsQueue.removeAll(checkedInCommits);
            }
        });
    }

	public void synchronizeFile(ScmContext scmContext, File modifiedFile, String comment, User user){
		if(scmManipulator == null || !scmManipulator.scmConfigurationSettledUp(scmContext, false)){
			return;
		}
		
		String message = "Synchronize file " + modifiedFile.getAbsolutePath();
		
		String modifiedFilePathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(modifiedFile);
		LOGGER.info("Synchronizing file ["+modifiedFilePathRelativeToHudsonRoot+"] to SCM ...");
		String commitMessage = createCommitMessage(scmContext, "Modification on file", user, comment);
		
		File modifiedFileTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+modifiedFilePathRelativeToHudsonRoot);
		boolean modifiedFileAlreadySynchronized = modifiedFileTranslatedInScm.exists();
		try {
			FileUtils.copyFile(modifiedFile, modifiedFileTranslatedInScm);
		} catch (IOException e) {
			LOGGER.throwing(FileUtils.class.getName(), "copyFile", e);
			LOGGER.severe("Error while copying file : "+e.getMessage());
			signal(message, false);
		}

		File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());
		
		List<File> synchronizedFiles = new ArrayList<File>();
		// if modified file is not yet synchronized with scm, let's add it !
		if(!modifiedFileAlreadySynchronized){
			synchronizedFiles.addAll(this.scmManipulator.addFile(scmRoot, modifiedFilePathRelativeToHudsonRoot));
		} else {
			synchronizedFiles.add(new File(modifiedFilePathRelativeToHudsonRoot));
		}

		boolean result = this.scmManipulator.checkinFiles(scmRoot, synchronizedFiles, commitMessage);
		
		if(result){
			LOGGER.info("Synchronized file ["+modifiedFilePathRelativeToHudsonRoot+"] to SCM !");
		}
		
		signal(message, result);
	}
	
	public void synchronizeAllConfigs(ScmContext scmContext, ScmSyncStrategy[] availableStrategies, User user){
		List<File> filesToSync = new ArrayList<File>();
		// Building synced files from strategies
		for(ScmSyncStrategy strategy : availableStrategies){
			filesToSync.addAll(strategy.createInitializationSynchronizedFileset());
		}
		
		for(File fileToSync : filesToSync){
			String hudsonConfigPathRelativeToHudsonRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(fileToSync);
			File hudsonConfigTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+hudsonConfigPathRelativeToHudsonRoot);
			try {
				if(!hudsonConfigTranslatedInScm.exists() 
						|| !FileUtils.contentEquals(hudsonConfigTranslatedInScm, fileToSync)){
					synchronizeFile(scmContext, fileToSync, "Synchronization init", user);
				}
			} catch (IOException e) {
			}
		}
	}

	public boolean scmCheckoutDirectorySettledUp(ScmContext scmContext){
		return scmManipulator != null && this.scmManipulator.scmConfigurationSettledUp(scmContext, false) && this.checkoutSucceeded;
	}
	
	public List<File> reloadAllFilesFromScm() throws IOException, ScmException {
		this.scmManipulator.update(new File(getCheckoutScmDirectoryAbsolutePath()));
		return syncDirectories(new File(getCheckoutScmDirectoryAbsolutePath() + File.separator), "");
	}
	
	private List<File> syncDirectories(File from, String relative) throws IOException {
		List<File> l = new ArrayList<File>();
		for(File f : from.listFiles()) {
			String newRelative = relative + File.separator + f.getName();
			File jenkinsFile = new File(Hudson.getInstance().getRootDir() + newRelative);
			if (f.getName().equals(scmManipulator.getScmSpecificFilename())) {
				// nothing to do
			}
			else if (f.isDirectory()) {
				if (!jenkinsFile.exists()) {
					FileUtils.copyDirectory(f, jenkinsFile, new FileFilter() {
						
						public boolean accept(File f) {
							return !f.getName().equals(scmManipulator.getScmSpecificFilename());
						}
						
					});
					l.add(jenkinsFile);
				}
				else {
					l.addAll(syncDirectories(f, newRelative));
				}
			}
			else {
				if (!jenkinsFile.exists() || !FileUtils.contentEquals(f, jenkinsFile)) {
					FileUtils.copyFile(f, jenkinsFile);
					l.add(jenkinsFile);
				}	
			}
		}
		return l;
	}

	private void signal(String operation, boolean result) {
		if (result) {
			getScmSyncConfigurationStatusManager().signalSuccess();
		}
		else {
			getScmSyncConfigurationStatusManager().signalFailed(operation);
		}
	}
	
    private static String createCommitMessage(ScmContext context, String messagePrefix, User user, String comment){
   		StringBuilder commitMessage = new StringBuilder();
   		commitMessage.append(messagePrefix);
   		if(user != null){
   			commitMessage.append(" by ").append(user.getId());
   		}
   		if(comment != null){
   			commitMessage.append(" with following comment : ").append(comment);
   		}
   		String message = commitMessage.toString();

           if(context.getCommitMessagePattern() == null || "".equals(context.getCommitMessagePattern())){
               return message;
           } else {
               return context.getCommitMessagePattern().replaceAll("\\[message\\]", message.replaceAll("\\$", "\\\\\\$"));
           }
   	}

	private static String getCheckoutScmDirectoryAbsolutePath(){
		return Hudson.getInstance().getRootDir().getAbsolutePath()+WORKING_DIRECTORY_PATH+CHECKOUT_SCM_DIRECTORY;
	}
}
