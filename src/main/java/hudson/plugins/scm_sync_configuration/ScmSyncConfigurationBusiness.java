package hudson.plugins.scm_sync_configuration;

import hudson.model.Hudson;
import hudson.model.User;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.add.AddScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.FileUtils;


public class ScmSyncConfigurationBusiness {

	private static final String WORKING_DIRECTORY_PATH = "/ScmSyncConfiguration/";
	private static final String CHECKOUT_SCM_DIRECTORY = "checkoutConfiguration";
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationBusiness.class.getName());
	
	private ScmSyncConfigurationPlugin plugin;
	private Embedder plexus;
	private ScmManager scmManager;
	private ScmRepository scmRepository = null;
	private File checkoutScmDirectory = null;
	
	public ScmSyncConfigurationBusiness(ScmSyncConfigurationPlugin plugin){
		this.plugin = plugin;
	}
	
	public void start() throws Exception {
		this.plexus = new Embedder();
		this.plexus.start();
		
		this.scmManager = (ScmManager)this.plexus.lookup(ScmManager.ROLE);
		this.checkoutScmDirectory = new File(getCheckoutScmDirectoryAbsolutePath());
		initializeRepository(false);
	}
	
	public void stop() throws Exception {
		this.plexus.stop();
	}
	
	public void initializeRepository(boolean deleteCheckoutScmDir){
		// Let's check if everything is available to checkout sources
		if(scmConfigurationSettledUp()){
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
	
	public boolean scmConfigurationSettledUp(){
		String scmRepositoryUrl = plugin.getScmRepositoryUrl();
		if(scmRepositoryUrl == null){
			return false;
		}
		
		try {
			this.scmRepository = this.scmManager.makeScmRepository(scmRepositoryUrl);
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
	
	public void synchronizeFile(File modifiedFile, String comment, User user){
		if(!scmConfigurationSettledUp()){
			return;
		}
		
		String modifiedFilePathRelativeToHudsonRoot = buildPathRelativeToHudsonRoot(modifiedFile);
		StringBuilder commitMessage = new StringBuilder();
		commitMessage.append("Modification on file");
		if(user != null){
			commitMessage.append(" by ").append(user.getId());
		}
		if(comment != null){
			commitMessage.append(" with following comment : ").append(comment);
		}
		
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
			this.scmManager.checkIn(this.scmRepository, fileSet, commitMessage.toString());
		} catch (ScmException e) {
			LOGGER.throwing(ScmManager.class.getName(), "checkIn", e);
			// TODO: rethrow exception
		}
	}
	
	public void synchronizeAllJobsConfigs(User user){
		File hudsonJobsDirectory = new File(Hudson.getInstance().getRootDir().getAbsolutePath()+File.separator+"jobs");
		for(File hudsonJob : hudsonJobsDirectory.listFiles()){
			if(hudsonJob.isDirectory()){
				File hudsonJobConfig = new File(hudsonJob.getAbsoluteFile()+File.separator+"config.xml");
				String hudsonJobConfigPathRelativeToHudsonRoot = buildPathRelativeToHudsonRoot(hudsonJobConfig);
				File hudsonJobConfigTranslatedInScm = new File(getCheckoutScmDirectoryAbsolutePath()+File.separator+hudsonJobConfigPathRelativeToHudsonRoot);
				try {
					if(!hudsonJobConfigTranslatedInScm.exists() 
							|| !FileUtils.contentEquals(hudsonJobConfigTranslatedInScm, hudsonJobConfig)){
						synchronizeFile(hudsonJobConfig, "Synchronization init", user);
					}
				} catch (IOException e) {
				}
			}
		}
	}

	private String buildPathRelativeToHudsonRoot(File filePath){
		File hudsonRoot = Hudson.getInstance().getRootDir();
		if(!filePath.getAbsolutePath().startsWith(hudsonRoot.getAbsolutePath())){
			throw new IllegalArgumentException("Err ! File <"+filePath.getAbsolutePath()+"> seems not to reside in <"+hudsonRoot.getAbsolutePath()+"> !");
		}
		return filePath.getAbsolutePath().substring(hudsonRoot.getAbsolutePath().length()+1); // "+1" because we don't need ending file separator
	}
	
	private static String getCheckoutScmDirectoryAbsolutePath(){
		return Hudson.getInstance().getRootDir().getAbsolutePath()+WORKING_DIRECTORY_PATH+CHECKOUT_SCM_DIRECTORY;
	}
}
