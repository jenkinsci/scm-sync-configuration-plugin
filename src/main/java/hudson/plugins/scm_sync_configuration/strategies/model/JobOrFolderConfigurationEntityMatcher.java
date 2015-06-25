package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;
import hudson.model.AbstractItem;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A {@link ConfigurationEntityMatcher} for job and Cloudbees Folders plugin folder configuration files.
 * Matches config.xml located in jobs/project/config.xml (normal Jenkins), but also in nested directories
 * below that as long as the path has the pattern (jobs/someName/)+config.xml (Cloudbees directory structure).
 */
public class JobOrFolderConfigurationEntityMatcher implements ConfigurationEntityMatcher {

	private static final String JOBS_DIR_NAME = "jobs";
	private static final String CONFIG_FILE_NAME = "config.xml";
	
	private static final Pattern CONFIGS_TO_MATCH = Pattern.compile("(?:" + JOBS_DIR_NAME + "/[^/]+/)+" + CONFIG_FILE_NAME);

	private static final FileFilter DIRECTORIES = new FileFilter() {
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};
	
	public boolean matches(Saveable saveable, File file) {
		// The file may be null, indicating a deletion!
		if (saveable instanceof AbstractItem) {
			// Both jobs and folders are AbstractItems, which are Saveables.
			if (file == null) {
				// Deleted.
				file = ((AbstractItem) saveable).getConfigFile().getFile();
			}
			return CONFIGS_TO_MATCH.matcher(JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file)).matches();
		}
		return false;
	}

	public String[] matchingFilesFrom(File rootDirectory) {
		// PatternsEntityMatcher uses a org.apache.tools.ant.DirectoryScanner, which returns a (sorted) list of matching file paths
		// relative to the given root directory. Let's adhere to that specification, but only for our very special job config pattern.
		if (rootDirectory == null || !rootDirectory.isDirectory()) {
			return new String[0];
		}
		List<String> collected = new ArrayList<String>();
		// Compare https://github.com/jenkinsci/cloudbees-folder-plugin/blob/70a4d47314a36b54d522cae0a78b3c76d153e627/src/main/java/com/cloudbees/hudson/plugins/folder/Folder.java#L200
		scanForJobConfigs (new File (rootDirectory, JOBS_DIR_NAME), JOBS_DIR_NAME, collected);
		String[] result = collected.toArray(new String[collected.size()]);
		Arrays.sort(result);
		return result;
	}

	private static void scanForJobConfigs(File jobsDir, String prefix, Collection<String> result) {
		if (!jobsDir.isDirectory()) {
			return;
		}
		File[] projects = jobsDir.listFiles(DIRECTORIES);
		if (projects == null) {
			return;
		}
		for (File project : projects) {
			if (new File(project, CONFIG_FILE_NAME).isFile()) {
				result.add(prefix + '/' + project.getName() + '/' + CONFIG_FILE_NAME);
				scanForJobConfigs(new File(project, JOBS_DIR_NAME), prefix + '/' + project.getName() + '/' + JOBS_DIR_NAME, result);
			}
			// Again, compare https://github.com/jenkinsci/cloudbees-folder-plugin/blob/70a4d47314a36b54d522cae0a78b3c76d153e627/src/main/java/com/cloudbees/hudson/plugins/folder/Folder.java#L200
			// The Cloudbees Folders plugin prunes the hierarchy on directories not containing a config.xml.
		}
	}
	
	public List<String> getIncludes() {
		// XXX The call hierarchy indicates that although this is called, ScmSyncConfigurationPlugin.getDefaultIncludes() isn't,
		// and thus this whole method and assembling strings is futile. Let's just return an empty list as other matchers
		// return ant patters, but there is no ant pattern that corresponds to our regular expression.
		return Collections.emptyList();
	}

}
