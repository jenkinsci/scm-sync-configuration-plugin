package hudson.plugins.scm_sync_configuration.strategies.model;

import hudson.model.Saveable;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.types.selectors.FileSelector;

/**
 * A matcher that matches specific files under $JENKINS_HOME.
 */
public interface ConfigurationEntityMatcher {
	
	/**
	 * Determines whether the matcher matches a given combination of saveable and file.
	 * 
	 * @param saveable the file belongs to
	 * @param file that is to be matched
	 * @return {@code true} on match, {@code false} otherwise
	 */
	public boolean matches(Saveable saveable, File file);
	
	/**
	 * Determines whether the matcher would have matched a deleted file, of which we know only its path and possibly whether it was directory.
	 * 
	 * @param saveable the file belonged to
	 * @param pathRelativeToRoot of the file or directory (which Jenkins has already deleted)
	 * @param isDirectory {@code true} if it's known that the path referred to a directory, {@code false} otherwise
	 * @return {@code true} on match, {@code false} otherwise
	 */
	public boolean matches(Saveable saveable, String pathRelativeToRoot, boolean isDirectory);
	
	/**
	 * Collects all files under the given rootDirectory that match, restricted by the given {@code link FileSelector}.
	 * 
	 * @param rootDirectory to traverse
	 * @param selector restricting the traversal
	 * @return an array of all path names relative to the rootDirectory of all files that match.
	 */
    public String[] matchingFilesFrom(File rootDirectory, FileSelector selector);
    
    /**
     * All patterns this matcher matches; used only for informational purposes in the UI.
     * 
     * @return A list of explanatory messages about the pattern the matcher matches.
     */
    List<String> getIncludes();
}
