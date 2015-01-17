package hudson.plugins.scm_sync_configuration;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import org.codehaus.plexus.util.FileUtils;

public class ScmSyncConfigurationStatusManager {

	private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationStatusManager.class.getName());

	public static final String LOG_SUCCESS_FILENAME = "scm-sync-configuration.success.log";
	
	public static final String LOG_FAIL_FILENAME = "scm-sync-configuration.fail.log";
	
	private File fail;
	private File success;
	
	public ScmSyncConfigurationStatusManager() {
		fail = new File(Jenkins.getInstance().getRootDir().getAbsolutePath()+File.separator+LOG_FAIL_FILENAME);
		success = new File(Jenkins.getInstance().getRootDir().getAbsolutePath()+File.separator+LOG_SUCCESS_FILENAME);
	}

	public String getLastFail() {
		return readFile(fail);
	}

	public String getLastSuccess() {
		return readFile(success);
	}
	
	public void signalSuccess() {
		writeFile(success, new Date().toString());
	}
	
	public void signalFailed(String description) {
		appendFile(fail, new Date().toString() + " : " + description + "<br/>");
	}

	private static String readFile(File f) {
		try {
			if(f.exists()) {
				return FileUtils.fileRead(f);
			}
		}
		catch(IOException e) {
			LOGGER.severe("Unable to read file " + f.getAbsolutePath() + " : " + e.getMessage());
		}
		return null;
	}
	
	private static void writeFile(File f, String data) {
		try {
			FileUtils.fileWrite(f.getAbsolutePath(), data);
		}
		catch(IOException e) {
			LOGGER.severe("Unable to write file " + f.getAbsolutePath() + " : " + e.getMessage());
		}
	}
	
	private static void appendFile(File f, String data) {
		try {
			FileUtils.fileAppend(f.getAbsolutePath(), data);
		}
		catch(IOException e) {
			LOGGER.severe("Unable to write file " + f.getAbsolutePath() + " : " + e.getMessage());
		}
	}

    public void purgeFailLogs() {
        fail.delete();
    }
}
