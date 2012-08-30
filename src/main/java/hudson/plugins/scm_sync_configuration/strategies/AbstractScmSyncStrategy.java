package hudson.plugins.scm_sync_configuration.strategies;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import hudson.XmlFile;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractScmSyncStrategy implements ScmSyncStrategy {

    private static final Function<String,File> PATH_TO_FILE_IN_HUDSON = new Function<String, File>() {
        public File apply(@Nullable String path) {
            return new File(Hudson.getInstance().getRootDir()+File.separator+path);
        }
    };

    protected static class DefaultCommitMessageFactory implements CommitMessageFactory {
        public WeightedMessage getMessageWhenSaveableUpdated(Saveable s, XmlFile file) {
            return new WeightedMessage("Modification on configuration(s)", ChangeSet.MessageWeight.MINIMAL);
        }
        public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath) {
            return new WeightedMessage("Item renamed", ChangeSet.MessageWeight.MINIMAL);
        }
        public WeightedMessage getMessageWhenItemDeleted(Item item) {
            return new WeightedMessage("File hierarchy deleted", ChangeSet.MessageWeight.MINIMAL);
        }
    }

	private ConfigurationEntityMatcher configEntityMatcher;
	private List<PageMatcher> pageMatchers;
	
	protected AbstractScmSyncStrategy(ConfigurationEntityMatcher _configEntityMatcher, List<PageMatcher> _pageMatchers){
		this.configEntityMatcher = _configEntityMatcher;
		this.pageMatchers = _pageMatchers;
	}

    protected ConfigurationEntityMatcher createConfigEntityMatcher(){
        return configEntityMatcher;
    }
	
	public boolean isSaveableApplicable(Saveable saveable, File file) {
		return createConfigEntityMatcher().matches(saveable, file);
	}

	public PageMatcher getPageMatcherMatching(String url){
		String rootUrl = Hudson.getInstance().getRootUrlFromRequest();
		String cleanedUrl = null;
		if(url.startsWith(rootUrl)){
			cleanedUrl = url.substring(rootUrl.length());
		} else {
			cleanedUrl = url;
		}
		for(PageMatcher pm : pageMatchers){
			if(pm.getUrlRegex().matcher(cleanedUrl).matches()){
				return pm;
			}
		}
		return null;
	}

    public List<File> createInitializationSynchronizedFileset() {
        File hudsonRoot = Hudson.getInstance().getRootDir();
        String[] matchingFilePaths = createConfigEntityMatcher().matchingFilesFrom(hudsonRoot);
        return new ArrayList(Collections2.transform(Arrays.asList(matchingFilePaths), PATH_TO_FILE_IN_HUDSON));
    }

	public boolean isCurrentUrlApplicable(String url) {
		return getPageMatcherMatching(url)!=null;
	}

    public List<String> getSyncIncludes(){
        return createConfigEntityMatcher().getIncludes();
    }

    public CommitMessageFactory getCommitMessageFactory(){
        return new DefaultCommitMessageFactory();
    }
}
