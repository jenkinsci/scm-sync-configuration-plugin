package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PatternsEntityMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class JenkinsConfigScmSyncStrategy extends AbstractScmSyncStrategy {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ {
        // Global configuration page
        add(new PageMatcher("^configure$", "config"));
        // View configuration pages
        add(new PageMatcher("^(.+/)?view/[^/]+/configure$", "viewConfig"));
        add(new PageMatcher("^newView$", "createView"));
    } };
    
    private static final Pattern [] PATTERNS = new Pattern[]{
    	Pattern.compile("^config\\.xml$"),
    	Pattern.compile("^hudson[^\\/]+\\.xml$")
    };
    
	private static final ConfigurationEntityMatcher CONFIG_ENTITY_MATCHER = new PatternsEntityMatcher(PATTERNS);
	
	public JenkinsConfigScmSyncStrategy(){
		super(CONFIG_ENTITY_MATCHER, PAGE_MATCHERS);
	}
	
	public List<File> createInitializationSynchronizedFileset() {
		return new ArrayList<File>(){{
			File root = new File(Hudson.getInstance().getRootDir().getAbsolutePath());
			for(String f : root.list()) {
				for(Pattern pattern : PATTERNS) {
					if (pattern.matcher(f).matches()) {
						add(new File(root.getAbsolutePath()+File.separator+f));
					}
				}
			}
		}};
	}
}
