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

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.command.update.UpdateScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.FileUtils;


public class ScmSyncConfigurationBusiness {

	private static final String WORKING_DIRECTORY_PATH = "/scm-sync-configuration/";
	private static final String CHECKOUT_SCM_DIRECTORY = "checkoutConfiguration";
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationBusiness.class.getName());
	
	private Embedder plexus;
	private ScmManager scmManager;
	private ScmRepository scmRepository = null;
	private File checkoutScmDirectory = null;
	
	public ScmSyncConfigurationBusiness(){
	}
	
	public void start() throws Exception {
		this.plexus = new Embedder();
		this.plexus.start();
	}
	
	public void init(ScmContext scmContext) throws Exception {
		this.scmManager = (ScmManager)this.plexus.lookup(ScmManager.ROLE);
		this.checkoutScmDirectory = new File(getCheckoutScmDirectoryAbsolutePath());
		initializeRepository(scmContext, false);
	}
	
	public void stop() throws Exception {
		this.plexus.stop();
	}
	
	public void initializeRepository(ScmContext scmContext, boolean deleteCheckoutScmDir){
		// Let's check if everything is available to checkout sources
		if(scmConfigurationSettledUp(scmContext)){
			// If checkoutScmDirectory was not empty, reinitialize it !
			if(deleteCheckoutScmDir && checkoutScmDirectory.exists()){
				try {
					FileUtils.forceDelete(checkoutScmDirectory);
				} catch (IOException e) {
					LOGGER.throwing(FileUtils.class.getName(), "forceDelete", e);
				}
			}
			
			// Checkouting sources
			try {
				scmManager.checkOut(this.scmRepository, new ScmFileSet(checkoutScmDirectory));
			} catch (ScmException e) {
				LOGGER.throwing(ScmManager.class.getName(), "checkOut", e);
			}
		}
	}
	
	public boolean scmConfigurationSettledUp(ScmContext scmContext){
		String scmRepositoryUrl = scmContext.getScmRepositoryUrl();
		if(scmRepositoryUrl == null){
			return false;
		}
		
		try {
			this.scmRepository = scmContext.getScm().getConfiguredRepository(this.scmManager, scmRepositoryUrl);
		} catch (ScmRepositoryException e) {
		} catch (NoSuchScmProviderException e) {
		}
		
		if(!checkoutScmDirectory.exists()){
			try {
				FileUtils.forceMkdir(checkoutScmDirectory);
			} catch (IOException e) {
				LOGGER.warning("Directory <"+ checkoutScmDirectory.getAbsolutePath() +"> cannot be created !");
				return false;
			}
		}
		
		return this.scmRepository!=null;
	}
	
	public void deleteHierarchy(ScmContext scmContext, File rootHierarchy, User user){
		deleteHierarchy(scmContext, rootHierarchy, createCommitMessage("Hierarchy deleted", user, null));
	}
	
	public void deleteHierarchy(ScmContext scmContext, File rootHierarchy, String commitMessage){
		if(!scmConfigurationSettledUp(scmContext)){
			return;
		}
		
		File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());
		String rootHierarchyPathRelativeToHudsonRoot = HudsonFilesHelper.buildPathRelativeToHudsonRoot(rootHierarchy);
		File rootHierarchyTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+rootHierarchyPathRelativeToHudsonRoot);
		File enclosingDirectory = rootHierarchyTranslatedInScm.getParentFile();
		try {
			// FIXME : CRITICAL !!
			// Here we MUST make an update to make scm delete work...
			// But generally, we should NOT force an update here (update should ONLY come from
			// the user will, not on the delete process !)
			// If we don't make an update, delete is complaining because of a tree conflict :(
			UpdateScmResult updateResult;
			ScmFileSet updateFileSet = new ScmFileSet(enclosingDirectory, rootHierarchy.getName());
			updateResult = this.scmManager.update(this.scmRepository, updateFileSet);
			ScmFileSet deleteFileSet = new ScmFileSet(enclosingDirectory, rootHierarchyTranslatedInScm);
			RemoveScmResult removeResult = this.scmManager.remove(this.scmRepository, deleteFileSet, commitMessage);
			ScmFileSet commitFileSet = new ScmFileSet(enclosingDirectory, rootHierarchy.getName());
			CheckInScmResult checkInResult = this.scmManager.checkIn(this.scmRepository, commitFileSet, commitMessage);
			updateResult = this.scmManager.update(this.scmRepository, updateFileSet);
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "remove", e);
			// TODO: rethrow exception
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void renameHierarchy(ScmContext scmContext, File oldDir, File newDir, User user){
		if(!scmConfigurationSettledUp(scmContext)){
			return;
		}
		
		String oldDirPathRelativeToHudsonRoot = HudsonFilesHelper.buildPathRelativeToHudsonRoot(oldDir);
		String newDirPathRelativeToHudsonRoot = HudsonFilesHelper.buildPathRelativeToHudsonRoot(newDir);
		String commitMessage = createCommitMessage("Moved "+oldDirPathRelativeToHudsonRoot+" hierarchy to "+newDirPathRelativeToHudsonRoot, user, null);
		
		File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());
		File newDirTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+newDirPathRelativeToHudsonRoot);
		try {
			newDirTranslatedInScm.mkdir(); // TODO: exception if mkdir returns false ?
			exportDirTo(oldDirPathRelativeToHudsonRoot, newDirTranslatedInScm);
			ScmFileSet addFileset = new ScmFileSet(scmRoot, new File(newDirPathRelativeToHudsonRoot));
			AddScmResult addResult = this.scmManager.add(this.scmRepository, addFileset);
			ScmFileSet addFilesetWithIncludes = new ScmFileSet(scmRoot, newDirPathRelativeToHudsonRoot+"/**/*");
			addResult = this.scmManager.add(this.scmRepository, addFilesetWithIncludes);
			CheckInScmResult checkInResult = this.scmManager.checkIn(this.scmRepository, addFileset, commitMessage);
			this.deleteHierarchy(scmContext, oldDir, commitMessage);
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "add, export or remove", e);
			// TODO: rethrow exception
		} catch (IOException e) {
			LOGGER.throwing(ScmFileSet.class.getName(), "<init>", e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void exportDirTo(String pathToExportRelativeToScmRoot, File notSynchronizedWithScmToDirectory) throws ScmException, IOException {
		File tmpDir = createTmpDir();
		try {
			this.scmManager.export(this.scmRepository, new ScmFileSet(tmpDir));
			FileUtils.copyDirectoryStructure(new File(tmpDir.getAbsolutePath()+File.separator+pathToExportRelativeToScmRoot), notSynchronizedWithScmToDirectory);
		}finally {
			tmpDir.delete();
		}
	}
	
	private static File createTmpDir() throws IOException {
	    final File temp = File.createTempFile("tmp", Long.toString(System.nanoTime()));
	    if(!(temp.delete())) { throw new IOException("Could not delete temp file: " + temp.getAbsolutePath()); }
	    if(!(temp.mkdir())) { throw new IOException("Could not create temp directory: " + temp.getAbsolutePath()); }
	    return (temp);
	}
	
	public void synchronizeFile(ScmContext scmContext, File modifiedFile, String comment, User user){
		if(!scmConfigurationSettledUp(scmContext)){
			return;
		}
		
		String modifiedFilePathRelativeToHudsonRoot = HudsonFilesHelper.buildPathRelativeToHudsonRoot(modifiedFile);
		String commitMessage = createCommitMessage("Modification on file", user, comment);
		
		File modifiedFileTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+modifiedFilePathRelativeToHudsonRoot);
		boolean modifiedFileAlreadySynchronized = modifiedFileTranslatedInScm.exists();
		try {
			FileUtils.copyFile(modifiedFile, modifiedFileTranslatedInScm);
		} catch (IOException e) {
			LOGGER.throwing(FileUtils.class.getName(), "copyFile", e);
			// TODO: rethrow exception
		}

		// if modified file is not yet synchronized with scm, let's add it !
		File scmRoot = new File(getCheckoutScmDirectoryAbsolutePath());
		List<File> synchronizedFiles = new ArrayList<File>();
		if(!modifiedFileAlreadySynchronized){
			try {
				// Split every directory leading through modifiedFilePathRelativeToHudsonRoot
				// and try add it in the scm
				String[] pathChunks = modifiedFilePathRelativeToHudsonRoot.split("\\\\|/");
				StringBuilder currentPath = new StringBuilder();
				for(int i=0; i<pathChunks.length-1; i++){
					currentPath.append(pathChunks[i]).append(File.separator);
					File currentFile = new File(currentPath.toString());
					// Trying to add current path to the scm ...
					AddScmResult addResult = this.scmManager.add(this.scmRepository, new ScmFileSet(scmRoot, currentFile));
					// If current has not yet been synchronized, addResult.isSuccess() should be true
					if(addResult.isSuccess()){
						synchronizedFiles.add(currentFile);
					}
				}
				// Adding file
				currentPath.append(pathChunks[pathChunks.length-1]);
				this.scmManager.add(this.scmRepository, new ScmFileSet(scmRoot, new File(currentPath.toString())));
			} catch (ScmException e) {
				LOGGER.throwing(ScmManager.class.getName(), "add", e);
				// TODO: rethrow exception
			}
		}
		synchronizedFiles.add(new File(modifiedFilePathRelativeToHudsonRoot));
		ScmFileSet fileSet = new ScmFileSet(scmRoot, synchronizedFiles);
		
		// Let's commit everything !
		try {
			this.scmManager.checkIn(this.scmRepository, fileSet, commitMessage);
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "checkIn", e);
			// TODO: rethrow exception
		}
	}
	
	public void synchronizeAllConfigs(ScmContext scmContext, ScmSyncStrategy[] availableStrategies, User user){
		List<File> filesToSync = new ArrayList<File>();
		// Building synced files from strategies
		for(ScmSyncStrategy strategy : availableStrategies){
			filesToSync.addAll(strategy.createInitializationSynchronizedFileset());
		}
		
		for(File fileToSync : filesToSync){
			String hudsonConfigPathRelativeToHudsonRoot = HudsonFilesHelper.buildPathRelativeToHudsonRoot(fileToSync);
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

	private static String createCommitMessage(String messagePrefix, User user, String comment){
		StringBuilder commitMessage = new StringBuilder();
		commitMessage.append(messagePrefix);
		if(user != null){
			commitMessage.append(" by ").append(user.getId());
		}
		if(comment != null){
			commitMessage.append(" with following comment : ").append(comment);
		}
		return commitMessage.toString();
	}
	
	private static String getCheckoutScmDirectoryAbsolutePath(){
		return Hudson.getInstance().getRootDir().getAbsolutePath()+WORKING_DIRECTORY_PATH+CHECKOUT_SCM_DIRECTORY;
	}
}
