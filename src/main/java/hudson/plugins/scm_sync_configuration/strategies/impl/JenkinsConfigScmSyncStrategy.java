package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ClassOnlyConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JenkinsConfigScmSyncStrategy extends AbstractScmSyncStrategy {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ { add(new PageMatcher("^configure$", "config")); } };
	private static final ConfigurationEntityMatcher CONFIG_ENTITY_MATCHER = new ClassOnlyConfigurationEntityMatcher(Hudson.class);
	
	public JenkinsConfigScmSyncStrategy(){
		super(CONFIG_ENTITY_MATCHER, PAGE_MATCHERS);
	}
	
	public List<File> createInitializationSynchronizedFileset() {
		return new ArrayList<File>(){{ 
			add(new File(Hudson.getInstance().getRootDir().getAbsolutePath()+File.separator+"config.xml")); 
			}};
	}
}
