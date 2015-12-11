package hudson.plugins.scm_sync_configuration;

import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;
import org.apache.maven.scm.*;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class providing atomic scm commands and wrapping calls to maven scm api
 * with logging informations
 * @author fcamblor
 */
public class SCMManipulator {

    private static final Logger LOGGER = Logger.getLogger(SCMManipulator.class.getName());

	private ScmManager scmManager;
	private ScmRepository scmRepository = null;
	private String scmSpecificFilename = null;
    private String scmGitBranch;

	public SCMManipulator(ScmManager _scmManager) {
		this.scmManager = _scmManager;
	}
	
	/**
	 * Will check if everything is settled up (useful before a scm manipulation)
	 * @param scmContext
	 * @param resetScmRepository
	 * @return
	 */
	public boolean scmConfigurationSettledUp(ScmContext scmContext, boolean resetScmRepository){
		String scmRepositoryUrl = scmContext.getScmRepositoryUrl();
		SCM scm = scmContext.getScm();
		if(scmRepositoryUrl == null || scm == null){
			return false;
		}
		
		if(resetScmRepository){
			LOGGER.info("Creating scmRepository connection data ..");
			this.scmRepository = scm.getConfiguredRepository(this.scmManager, scmRepositoryUrl);
			try {
				this.scmSpecificFilename = this.scmManager.getProviderByRepository(this.scmRepository).getScmSpecificFilename();
			}
			catch(NoSuchScmProviderException e) {
				LOGGER.throwing(ScmManager.class.getName(), "getScmSpecificFilename", e);
				LOGGER.severe("[getScmSpecificFilename] Error while getScmSpecificFilename : "+e.getMessage());
				return false;
			}
		}

        scmGitBranch = scmContext.getScmGitBranch();
		return expectScmRepositoryInitiated();
	}
	
	private boolean expectScmRepositoryInitiated(){
		boolean scmRepositoryInitiated = this.scmRepository != null;
		if(!scmRepositoryInitiated) LOGGER.warning("SCM Repository has not yet been initiated !");
		return scmRepositoryInitiated;
	}
	
	public UpdateScmResult update(File root) throws ScmException {
		return this.scmManager.update(scmRepository, new ScmFileSet(root));
	}
	public boolean checkout(File checkoutDirectory){
		boolean checkoutOk = false;
		
		if(!expectScmRepositoryInitiated()){
			return checkoutOk;
		}
		
		// Checkouting sources
		LOGGER.fine("Checkouting SCM files into ["+checkoutDirectory.getAbsolutePath()+"] ...");
		try {
            ScmBranch scmBranch = null;
            if (scmGitBranch != null && scmGitBranch.length() > 0){
                scmBranch = new ScmBranch(scmGitBranch);
            }
            CheckOutScmResult result = scmManager.checkOut(this.scmRepository, new ScmFileSet(checkoutDirectory),scmBranch);

			if(!result.isSuccess()){
				LOGGER.severe("[checkout] Error during checkout : "+result.getProviderMessage()+" || "+result.getCommandOutput());
				return checkoutOk;
			}
			checkoutOk = true;
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "checkOut", e);
			LOGGER.severe("[checkout] Error during checkout : "+e.getMessage());
			return checkoutOk;
		}
		
		if(checkoutOk){
			LOGGER.fine("Checkouted SCM files into ["+checkoutDirectory.getAbsolutePath()+"] !");
		}

		return checkoutOk;
	}

    public List<File> deleteHierarchy(File hierarchyToDelete){
        if(!expectScmRepositoryInitiated()){
            return null;
        }

        File enclosingDirectory = hierarchyToDelete.getParentFile();

        LOGGER.fine("Deleting SCM hierarchy ["+hierarchyToDelete.getAbsolutePath()+"] from SCM ...");

        File commitFile = hierarchyToDelete;
        while(! commitFile.isDirectory()) {
            commitFile = commitFile.getParentFile();
        }

        try {
            ScmFileSet deleteFileSet = new ScmFileSet(enclosingDirectory, hierarchyToDelete);
            RemoveScmResult removeResult = this.scmManager.remove(this.scmRepository, deleteFileSet, "");
            if(!removeResult.isSuccess()){
                LOGGER.severe("[deleteHierarchy] Problem during remove : "+removeResult.getProviderMessage());
                return null;
            }

            List<File> filesToCommit = new ArrayList<File>();
            filesToCommit.add(commitFile);

            filesToCommit = refineUpdatedFilesInScmResult(filesToCommit);
            return filesToCommit;
        } catch (ScmException e) {
            LOGGER.throwing(ScmManager.class.getName(), "remove", e);
            LOGGER.severe("[deleteHierarchy] Hierarchy deletion aborted : "+e.getMessage());
            return null;
        }
    }

	public List<File> addFile(File scmRoot, String filePathRelativeToScmRoot){
		List<File> synchronizedFiles = new ArrayList<File>();
		
		if(!expectScmRepositoryInitiated()){
			return synchronizedFiles;
		}
		
		LOGGER.fine("Adding SCM file ["+filePathRelativeToScmRoot+"] ...");
		
		try {
			// Split every directory leading through modifiedFilePathRelativeToHudsonRoot
			// and try add it in the scm
			String[] pathChunks = filePathRelativeToScmRoot.split("\\\\|/");
			StringBuilder currentPath = new StringBuilder();
			for(int i=0; i<pathChunks.length; i++){
				currentPath.append(pathChunks[i]);
				if(i != pathChunks.length-1){
					currentPath.append(File.separator);
				}
				File currentFile = new File(currentPath.toString());

				// Trying to add current path to the scm ...
				AddScmResult addResult = this.scmManager.add(this.scmRepository, new ScmFileSet(scmRoot, currentFile));
				// If current has not yet been synchronized, addResult.isSuccess() should be true
				if(addResult.isSuccess()){
					synchronizedFiles.addAll(refineUpdatedFilesInScmResult(addResult.getAddedFiles()));

                    if(i == pathChunks.length-1 && new File(scmRoot.getAbsolutePath()+File.separator+currentPath.toString()).isDirectory()){
                        addResult = this.scmManager.add(this.scmRepository, new ScmFileSet(scmRoot, currentPath.toString()+"/**/*"));
                        if(addResult.isSuccess()){
                            synchronizedFiles.addAll(refineUpdatedFilesInScmResult(addResult.getAddedFiles()));
                        } else {
                            LOGGER.severe("Error while adding SCM files in directory : " + addResult.getCommandOutput());
                        }
                    }
				} else {
                    // If addResult.isSuccess() is false, it isn't an error if it is related to path chunks (except for latest one) :
                    // if pathChunk is already synchronized, addResult.isSuccess() will be false.
                    Level logLevel = (i==pathChunks.length-1)?Level.SEVERE:Level.FINE;
                    LOGGER.log(logLevel, "Error while adding SCM file : " + addResult.getCommandOutput());
                }
			}
        } catch (IOException e) {
            LOGGER.throwing(ScmFileSet.class.getName(), "init<>", e);
            LOGGER.warning("[addFile] Error while creating ScmFileset : "+e.getMessage());
            return synchronizedFiles;
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "add", e);
			LOGGER.warning("[addFile] Error while adding file : "+e.getMessage());
			return synchronizedFiles;
		}


		if(!synchronizedFiles.isEmpty()){
			LOGGER.fine("Added SCM files : "+Arrays.toString(synchronizedFiles.toArray(new File[0]))+" !");
		}
		
		return synchronizedFiles;
	}

    private List<File> refineUpdatedFilesInScmResult(List updatedFiles){
        List<File> refinedUpdatedFiles = new ArrayList<File>();

        // Cannot use directly a List<ScmFile> or List<File> here, since result type will depend upon
        // current scm api version
        Iterator scmFileIter = updatedFiles.iterator();
        while(scmFileIter.hasNext()){
            Object scmFile = scmFileIter.next();
            if(scmFile instanceof File){
                String checkoutScmDir = ScmSyncConfigurationBusiness.getCheckoutScmDirectoryAbsolutePath();
                String scmPath = ((File) scmFile).getAbsolutePath();
                if(scmPath.startsWith(checkoutScmDir)){
                    scmPath = scmPath.substring(checkoutScmDir.length() + 1);
                }
                refinedUpdatedFiles.add(new File(scmPath));
            } else if(scmFile instanceof ScmFile){
                refinedUpdatedFiles.add(new File(((ScmFile)scmFile).getPath()));
            } else {
                LOGGER.severe("Unhandled AddScmResult.addedFiles type : " + scmFile.getClass().getName());
            }
        }

        return refinedUpdatedFiles;
    }
	
	public boolean checkinFiles(File scmRoot, String commitMessage){
		boolean checkinOk = false;

		if(!expectScmRepositoryInitiated()){
			return checkinOk;
		}

		LOGGER.fine("Checking in SCM files ...");

		ScmFileSet fileSet = new ScmFileSet(scmRoot);

		// Let's commit everything !
		try {
			CheckInScmResult result = this.scmManager.checkIn(this.scmRepository, fileSet, commitMessage);
			if(!result.isSuccess()){
				LOGGER.severe("[checkinFiles] Problem during SCM commit : "+result.getCommandOutput());
				return checkinOk;
			}
			checkinOk = true;
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "checkIn", e);
			LOGGER.severe("[checkinFiles] Error while checkin : "+e.getMessage());
			return checkinOk;
		}


		if(checkinOk){
			LOGGER.fine("Checked in SCM files !");
		}
		
		return checkinOk;
	}

	public String getScmSpecificFilename() {
		return scmSpecificFilename;
	}
}
