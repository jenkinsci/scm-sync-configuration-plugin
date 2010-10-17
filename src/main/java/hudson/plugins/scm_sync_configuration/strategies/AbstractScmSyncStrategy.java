package hudson.plugins.scm_sync_configuration.strategies;

import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.io.File;
import java.util.List;

public abstract class AbstractScmSyncStrategy implements ScmSyncStrategy {

	private ConfigurationEntityMatcher configEntityMatcher;
	private List<PageMatcher> pageMatchers;
	
	protected AbstractScmSyncStrategy(ConfigurationEntityMatcher _configEntityMatcher, List<PageMatcher> _pageMatchers){
		this.configEntityMatcher = _configEntityMatcher;
		this.pageMatchers = _pageMatchers;
	}
	
	public boolean isSaveableApplicable(Saveable saveable, File file) {
		return configEntityMatcher.matches(saveable, file);
	}

	public PageMatcher getPageMatcherMatching(String url){
		String rootUrl = Hudson.getInstance().getRootUrl();
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
	
	public boolean isCurrentUrlApplicable(String url) {
		return getPageMatcherMatching(url)!=null;
	}

}
