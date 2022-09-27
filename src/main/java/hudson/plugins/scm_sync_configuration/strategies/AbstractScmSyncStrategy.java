package hudson.plugins.scm_sync_configuration.strategies;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.model.MessageWeight;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.selectors.FileSelector;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jenkins.model.Jenkins;

public abstract class AbstractScmSyncStrategy implements ScmSyncStrategy {

    private static final Function<String,File> PATH_TO_FILE_IN_HUDSON = new Function<String, File>() {
        @Override
        public File apply(@Nullable String path) {
            return new File(Jenkins.getInstance().getRootDir(), path);
        }
    };

    protected static class DefaultCommitMessageFactory implements CommitMessageFactory {
        @Override
        public WeightedMessage getMessageWhenSaveableUpdated(Saveable s, XmlFile file) {
            return new WeightedMessage("Modification on configuration(s)", MessageWeight.MINIMAL);
        }
        @Override
        public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath) {
            return new WeightedMessage("Item renamed", MessageWeight.MINIMAL);
        }
        @Override
        public WeightedMessage getMessageWhenItemDeleted(Item item) {
            return new WeightedMessage("File hierarchy deleted", MessageWeight.MINIMAL);
        }
    }

    private final ConfigurationEntityMatcher configEntityMatcher;
    private final List<PageMatcher> pageMatchers;
    private CommitMessageFactory commitMessageFactory;

    protected AbstractScmSyncStrategy(ConfigurationEntityMatcher _configEntityMatcher, List<PageMatcher> _pageMatchers){
        this.configEntityMatcher = _configEntityMatcher;
        this.pageMatchers = _pageMatchers;
    }

    protected ConfigurationEntityMatcher createConfigEntityMatcher(){
        return configEntityMatcher;
    }

    @Override
    public boolean isSaveableApplicable(Saveable saveable, File file) {
        return createConfigEntityMatcher().matches(saveable, file);
    }

    public PageMatcher getPageMatcherMatching(String url){
        String rootUrl = Jenkins.getInstance().getRootUrlFromRequest();
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

    @Override
    public List<File> collect() {
        return collect(null);
    }

    @Override
    public List<File> collect(File directory) {
        File jenkinsRoot = Jenkins.getInstance().getRootDir();
        if (jenkinsRoot.equals(directory)) {
            directory = null;
        }
        FileSelector selector = null;
        if (directory != null) {
            String pathRelativeToRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(directory);
            if (pathRelativeToRoot == null) {
                throw new IllegalArgumentException(directory.getAbsolutePath() + " is not under " + jenkinsRoot.getAbsolutePath());
            }
            pathRelativeToRoot = pathRelativeToRoot.replace('\\', '/');
            final String restrictedPath = pathRelativeToRoot.endsWith("/") ? pathRelativeToRoot : pathRelativeToRoot + '/';
            selector = new FileSelector() {
                @Override
                public boolean isSelected(File basedir, String pathRelativeToBasedir, File file) throws BuildException {
                    // Only include directories leading to our directory (parent directories and the directory itself) and then whatever is below.
                    pathRelativeToBasedir = pathRelativeToBasedir.replace('\\', '/');
                    if (file.isDirectory()) {
                        pathRelativeToBasedir = pathRelativeToBasedir.endsWith("/") ? pathRelativeToBasedir : pathRelativeToBasedir + '/';
                    }
                    return pathRelativeToBasedir.startsWith(restrictedPath) || restrictedPath.startsWith(pathRelativeToBasedir);
                }
            };
        }
        String[] matchingFilePaths = createConfigEntityMatcher().matchingFilesFrom(jenkinsRoot, selector);
        return new ArrayList<File>(Collections2.transform(Arrays.asList(matchingFilePaths), PATH_TO_FILE_IN_HUDSON));
    }

    @Override
    public boolean isCurrentUrlApplicable(String url) {
        return getPageMatcherMatching(url)!=null;
    }

    @Override
    public List<String> getSyncIncludes(){
        return createConfigEntityMatcher().getIncludes();
    }

    @Override
    public CommitMessageFactory getCommitMessageFactory(){
        if (commitMessageFactory == null) {
            commitMessageFactory = new DefaultCommitMessageFactory();
        }
        return commitMessageFactory;
    }
}
