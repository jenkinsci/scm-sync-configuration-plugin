package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;
import hudson.model.AbstractItem;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.selectors.FileSelector;

/**
 * A {@link ConfigurationEntityMatcher} for job and Cloudbees Folders plugin folder configuration files.
 * Matches config.xml located in jobs/project/config.xml (normal Jenkins), but also in nested directories
 * below that as long as the path has the pattern (jobs/someName/)+config.xml (Cloudbees directory structure).
 */
public class JobOrFolderConfigurationEntityMatcher extends PatternsEntityMatcher {

    private static final String JOBS_DIR_NAME = "jobs";
    private static final String CONFIG_FILE_NAME = "config.xml";
    private static final String DIRECTORY_REGEXP = "(?:" + JOBS_DIR_NAME + "/[^/]+/)+";

    private static final Pattern CONFIGS_TO_MATCH = Pattern.compile(DIRECTORY_REGEXP + CONFIG_FILE_NAME);
    private static final Pattern DIRECTORIES_TO_MATCH = Pattern.compile(DIRECTORY_REGEXP);

    private static final String[] ANT_PATTERN = new String[] { JOBS_DIR_NAME + "/**/" + CONFIG_FILE_NAME };

    public JobOrFolderConfigurationEntityMatcher() {
        super(ANT_PATTERN);
        // This pattern is only used for matchingFileFrom() below, and is augmented by a FileSelector enforcing the (jobs/someName/)+ pattern
    }

    @Override
    public boolean matches(Saveable saveable, File file) {
        // The file may be null, indicating a deletion!
        if (saveable instanceof AbstractItem) {
            // Both jobs and folders are AbstractItems, which are Saveables.
            if (file == null) {
                // Deleted.
                file = ((AbstractItem) saveable).getConfigFile().getFile();
            } else if (file.isDirectory()) {
                file = new File(file, CONFIG_FILE_NAME);
            }
            return matches(saveable, JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file), false);
        }
        return false;
    }

    @Override
    public boolean matches(Saveable saveable, String pathRelativeToRoot, boolean isDirectory) {
        if ((saveable instanceof AbstractItem) && pathRelativeToRoot != null) {
            pathRelativeToRoot = pathRelativeToRoot.replace('\\', '/');
            if (isDirectory) {
                if (!pathRelativeToRoot.endsWith("/")) {
                    pathRelativeToRoot += '/';
                }
                pathRelativeToRoot += CONFIG_FILE_NAME;
            }
            return CONFIGS_TO_MATCH.matcher(pathRelativeToRoot).matches();
        }
        return false;
    }

    @Override
    public List<String> getIncludes() {
        // This is used only for display in the UI.
        return Collections.singletonList(ANT_PATTERN[0] + " (** restricted to real project and folder directories)");
    }

    @Override
    public String[] matchingFilesFrom(File rootDirectory, FileSelector selector) {
        // Create a selector that enforces the (jobs/someName)+ pattern
        final FileSelector originalSelector = selector;
        FileSelector combinedSelector = new FileSelector() {
            @Override
            public boolean isSelected(File basedir, String pathRelativeToBaseDir, File file) throws BuildException {
                if (originalSelector != null && !originalSelector.isSelected(basedir, pathRelativeToBaseDir, file)) {
                    return false;
                }
                pathRelativeToBaseDir = pathRelativeToBaseDir.replace('\\', '/');
                if (CONFIGS_TO_MATCH.matcher(pathRelativeToBaseDir).matches()) {
                    return true;
                }

                if (JOBS_DIR_NAME.equals(pathRelativeToBaseDir)) {
                    return true;
                }
                if (!pathRelativeToBaseDir.endsWith("/")) {
                    pathRelativeToBaseDir += '/';
                }
                if (pathRelativeToBaseDir.endsWith('/' + JOBS_DIR_NAME + '/')) {
                    pathRelativeToBaseDir = StringUtils.removeEnd(pathRelativeToBaseDir, JOBS_DIR_NAME + '/');
                    return DIRECTORIES_TO_MATCH.matcher(pathRelativeToBaseDir).matches();
                } else {
                    // Compare https://github.com/jenkinsci/cloudbees-folder-plugin/blob/70a4d47314a36b54d522cae0a78b3c76d153e627/src/main/java/com/cloudbees/hudson/plugins/folder/Folder.java#L200
                    // The Cloudbees Folders plugin prunes the hierarchy on directories not containing a config.xml.
                    return DIRECTORIES_TO_MATCH.matcher(pathRelativeToBaseDir).matches() && file.isDirectory() && new File(file, CONFIG_FILE_NAME).exists();
                }
            }
        };
        return super.matchingFilesFrom(rootDirectory, combinedSelector);
    }

}
