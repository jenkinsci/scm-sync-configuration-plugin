package hudson.plugins.scm_sync_configuration.strategies;

import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.util.List;

public abstract class AbstractScmSyncStrategy<T extends Saveable> implements ScmSyncStrategy {

	private Class<? extends Saveable> saveableClazz;
	private List<PageMatcher> pageMatchers;
	
	protected AbstractScmSyncStrategy(Class<T> clazz, List<PageMatcher> _pageMatchers){
		this.saveableClazz = clazz;
		this.pageMatchers = _pageMatchers;
	}
	
	public boolean isSaveableApplicable(Saveable saveable) {
		return saveableClazz.isAssignableFrom(saveable.getClass());
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
