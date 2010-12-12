package hudson.plugins.scm_sync_configuration;

import hudson.plugins.scm_sync_configuration.model.ScmContext;
import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.export.ExportScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.codehaus.plexus.util.FileUtils;

/**
 * Class providing atomic scm commands and wrapping calls to maven scm api
 * with logging informations
 * @author fcamblor
 */
public class SCMManipulator {

    private static final Logger LOGGER = Logger.getLogger(SCMManipulator.class.getName());

	private ScmManager scmManager;
	private ScmRepository scmRepository = null;

	public SCMManipulator(ScmManager _scmManager){
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
		}
		
		return expectScmRepositoryInitiated();
	}
	
	private boolean expectScmRepositoryInitiated(){
		boolean scmRepositoryInitiated = this.scmRepository != null;
		if(!scmRepositoryInitiated) LOGGER.warning("SCM Repository has not yet been initiated !");
		return scmRepositoryInitiated;
	}
	
	public boolean checkout(File checkoutDirectory){
		boolean checkoutOk = false;
		
		if(!expectScmRepositoryInitiated()){
			return checkoutOk;
		}
		
		// Checkouting sources
		LOGGER.fine("Checkouting SCM files into <"+checkoutDirectory.getAbsolutePath()+"> ...");
		try {
			CheckOutScmResult result = scmManager.checkOut(this.scmRepository, new ScmFileSet(checkoutDirectory));
			if(!result.isSuccess()){
				LOGGER.severe("[checkout] Error during checkout : "+result.getProviderMessage());
				return checkoutOk;
			}
			checkoutOk = true;
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "checkOut", e);
			LOGGER.severe("[checkout] Error during checkout : "+e.getMessage());
			return checkoutOk;
		}
		
		if(checkoutOk){
			LOGGER.fine("Checkouted SCM files into <"+checkoutDirectory.getAbsolutePath()+"> !");
		}

		return checkoutOk;
	}
	
	public boolean deleteHierarchy(File hierarchyToDelete, String commitMessage){
		boolean deleteOk = false;
		
		if(!expectScmRepositoryInitiated()){
			return deleteOk;
		}
		
		String directoryName = hierarchyToDelete.getName();
		File enclosingDirectory = hierarchyToDelete.getParentFile();
		
		LOGGER.fine("Deleting SCM hierarchy <"+hierarchyToDelete.getAbsolutePath()+"> from SCM ...");
		ScmFileSet updateFileSet = null;
		ScmFileSet commitFileSet = null;
		try {
			updateFileSet = new ScmFileSet(enclosingDirectory, directoryName);
			commitFileSet = new ScmFileSet(enclosingDirectory, directoryName);
		}catch(IOException e){
			LOGGER.throwing(ScmFileSet.class.getName(), "<init>", e);
			LOGGER.severe("[deleteHierarchy] Hierarchy deletion aborted : "+e.getMessage());
			return deleteOk;
		}
		
		try {
			// FIXME : CRITICAL !! It could be lead to problems when 2-way synchronization will occur in the plugin !
			// Here we MUST make an update to make scm delete work...
			// But generally, we should NOT force an update here (update should ONLY come from
			// the user will, not on the delete process !)
			// If we don't make an update, delete is complaining because of a tree conflict :(
			UpdateScmResult updateResult = this.scmManager.update(this.scmRepository, updateFileSet);
			if(!updateResult.isSuccess()){
				LOGGER.severe("[deleteHierarchy] Problem during first update : "+updateResult.getProviderMessage());
				return deleteOk;
			}
			ScmFileSet deleteFileSet = new ScmFileSet(enclosingDirectory, hierarchyToDelete);
			RemoveScmResult removeResult = this.scmManager.remove(this.scmRepository, deleteFileSet, commitMessage);
			if(!removeResult.isSuccess()){
				LOGGER.severe("[deleteHierarchy] Problem during remove : "+removeResult.getProviderMessage());
				return deleteOk;
			}
			CheckInScmResult checkInResult = this.scmManager.checkIn(this.scmRepository, commitFileSet, commitMessage);
			if(!checkInResult.isSuccess()){
				LOGGER.severe("[deleteHierarchy] Problem during checkin : "+checkInResult.getProviderMessage());
				return deleteOk;
			}
			updateResult = this.scmManager.update(this.scmRepository, updateFileSet);
			if(!updateResult.isSuccess()){
				LOGGER.severe("[deleteHierarchy] Problem during second update : "+updateResult.getProviderMessage());
				return deleteOk;
			}
			deleteOk = true;
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "remove", e);
			LOGGER.severe("[deleteHierarchy] Hierarchy deletion aborted : "+e.getMessage());
			return deleteOk;
		}

		if(deleteOk){
			LOGGER.fine("Deleted SCM hierarchy <"+hierarchyToDelete.getAbsolutePath()+"> !");
		}

		return deleteOk;
	}
	
	public boolean renameHierarchy(File scmRoot, String oldDirPathRelativeToScmRoot, String newDirPathRelativeToScmRoot, String commitMessage){
		boolean renameOk = false;
		
		if(!expectScmRepositoryInitiated()){
			return renameOk;
		}
		
		LOGGER.fine("Renaming SCM hierarchy <"+oldDirPathRelativeToScmRoot+"> to <"+newDirPathRelativeToScmRoot+"> ...");
		File newDir = new File(scmRoot.getAbsoluteFile()+File.separator+newDirPathRelativeToScmRoot);
		File oldDir = new File(scmRoot.getAbsoluteFile()+File.separator+oldDirPathRelativeToScmRoot);
		
		try {
			if(!newDir.mkdir()){
				LOGGER.severe("[renameHierarchy] Failed to create <"+newDir.getAbsolutePath()+"> !");
				return renameOk;
			}
			if(!export(oldDirPathRelativeToScmRoot, newDir)){
				LOGGER.severe("[renameHierarchy] Failed to export <"+oldDirPathRelativeToScmRoot+"> !");
				return renameOk;
			}
			ScmFileSet addFileset = new ScmFileSet(scmRoot, new File(newDirPathRelativeToScmRoot));
			AddScmResult addResult = this.scmManager.add(this.scmRepository, addFileset);
			if(!addResult.isSuccess()){
				LOGGER.severe("[renameHierarchy] Failed to add <"+newDirPathRelativeToScmRoot+"> : "+addResult.getProviderMessage());
				return renameOk;
			}
			ScmFileSet addFilesetWithIncludes = new ScmFileSet(scmRoot, newDirPathRelativeToScmRoot+"/**/*");
			addResult = this.scmManager.add(this.scmRepository, addFilesetWithIncludes);
			if(!addResult.isSuccess()){
				LOGGER.severe("[renameHierarchy] Failed to add <"+newDirPathRelativeToScmRoot+"/**/*> : "+addResult.getProviderMessage());
				return renameOk;
			}
			CheckInScmResult checkInResult = this.scmManager.checkIn(this.scmRepository, addFileset, commitMessage);
			if(!checkInResult.isSuccess()){
				LOGGER.severe("[renameHierarchy] Failed to checkin : "+checkInResult.getProviderMessage());
				return renameOk;
			}
			if(!this.deleteHierarchy(oldDir, commitMessage)){
				LOGGER.severe("[renameHierarchy] Failed to deleteHierarchy <"+oldDir.getAbsolutePath()+"> !");
				return renameOk;
			}
			renameOk = true;
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "add, export or remove", e);
			LOGGER.severe("[renameHierarchy] Error during add, export or remove : "+e.getMessage());
			return renameOk;
		} catch (IOException e) {
			LOGGER.throwing(ScmFileSet.class.getName(), "<init>", e);
			LOGGER.severe("[renameHierarchy] Error during scmFileSet creation : "+e.getMessage());
			return renameOk;
		}
		
		if(renameOk){
			LOGGER.fine("Renamed SCM hierarchy <"+oldDirPathRelativeToScmRoot+"> to <"+newDirPathRelativeToScmRoot+"> !");
		}
		
		return renameOk;
	}
	
	public boolean export(String syncedHierarchyPathRelativeToScmRoot, File outputDirectory){
		boolean exportOk = false;
		
		if(!expectScmRepositoryInitiated()){
			return exportOk;
		}
		
		LOGGER.fine("Exporting SCM directory <"+syncedHierarchyPathRelativeToScmRoot+"> to <"+outputDirectory.getAbsolutePath()+"> ...");
		
		File tmpDir;
		try {
			tmpDir = createTmpDir();
		} catch (IOException e) {
			LOGGER.throwing(this.getClass().getName(), "createTmpDir", e);
			LOGGER.severe("[export] Error creating tmp directory : "+e.getMessage());
			return exportOk;
		}
		
		try {
			ExportScmResult result = this.scmManager.export(this.scmRepository, new ScmFileSet(tmpDir));
			if(!result.isSuccess()){
				LOGGER.severe("[export] Error exporting directory structure : "+result.getProviderMessage());
				return exportOk;
			}
			FileUtils.copyDirectoryStructure(new File(tmpDir.getAbsolutePath()+File.separator+syncedHierarchyPathRelativeToScmRoot), outputDirectory);
			exportOk = true;
		}catch(IOException e){
			LOGGER.throwing(FileUtils.class.getName(), "copyDirectoryStructure", e);
			LOGGER.severe("[export] Error copying directory structure : "+e.getMessage());
			return exportOk;
		}catch(ScmException e){
			LOGGER.throwing(this.scmManager.getClass().getName(), "export", e);
			LOGGER.severe("[export] Error exporting directory structure : "+e.getMessage());
			return exportOk;
		}finally {
			// Deleting tmp directory..
			if(!tmpDir.delete()){
				LOGGER.warning("[export] Failed to delete tmp directory !");
			}
		}

		if(exportOk){
			LOGGER.fine("Exported SCM directory <"+syncedHierarchyPathRelativeToScmRoot+"> to <"+outputDirectory.getAbsolutePath()+"> !");
		}
		
		return exportOk;
	}
	
	public List<File> addFile(File scmRoot, String filePathRelativeToScmRoot){
		List<File> synchronizedFiles = new ArrayList<File>();
		
		if(!expectScmRepositoryInitiated()){
			return synchronizedFiles;
		}
		
		LOGGER.fine("Adding SCM file <"+filePathRelativeToScmRoot+"> ...");
		
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
					synchronizedFiles.add(currentFile);
				}
			}
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
	
	public boolean checkinFiles(File scmRoot, List<File> filesToCheckin, String commitMessage){
		boolean checkinOk = false;

		if(!expectScmRepositoryInitiated()){
			return checkinOk;
		}
		
		LOGGER.fine("Checking in SCM files : "+Arrays.toString(filesToCheckin.toArray(new File[0]))+" ...");

		ScmFileSet fileSet = new ScmFileSet(scmRoot, filesToCheckin);

		// Let's commit everything !
		try {
			CheckInScmResult result = this.scmManager.checkIn(this.scmRepository, fileSet, commitMessage);
			if(!result.isSuccess()){
				LOGGER.severe("[checkinFiles] Problem during commit of <"+Arrays.toString(filesToCheckin.toArray(new File[0]))+"> : "+result.getCommandOutput());
				return checkinOk;
			}
			checkinOk = true;
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "checkIn", e);
			LOGGER.severe("[checkinFiles] Error while checkin : "+e.getMessage());
			return checkinOk;
		}

		
		if(checkinOk){
			LOGGER.fine("Checked in SCM files : "+Arrays.toString(filesToCheckin.toArray(new File[0]))+" !");
		}
		
		return checkinOk;
	}
	
	private static File createTmpDir() throws IOException {
	    final File temp = File.createTempFile("tmp", Long.toString(System.nanoTime()));
	    if(!(temp.delete())) { throw new IOException("Could not delete temp file: " + temp.getAbsolutePath()); }
	    if(!(temp.mkdir())) { throw new IOException("Could not create temp directory: " + temp.getAbsolutePath()); }
	    return (temp);
	}
}
