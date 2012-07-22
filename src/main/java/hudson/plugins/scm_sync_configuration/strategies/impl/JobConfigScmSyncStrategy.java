package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Job;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ClassAndFileConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.util.ArrayList;
import java.util.List;

public class JobConfigScmSyncStrategy extends AbstractScmSyncStrategy {

    // Don't miss to take into account view urls since we can configure a job through a view !
	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ {
        add(new PageMatcher("^(.*view/[^/]+/)?job/[^/]+/configure$", "form[name='config']"));
    } };
    // Only saving config.xml file located in job directory
    // Some plugins (like maven release plugin) could add their own configuration files in the job directory that we don't want to synchronize
    // ... at least in the current strategy !
	private static final String [] PATTERNS = new String[] {
        "jobs/*/config.xml"
	};
	private static final ConfigurationEntityMatcher CONFIG_ENTITY_MANAGER = new ClassAndFileConfigurationEntityMatcher(Job.class, PATTERNS);
	
	public JobConfigScmSyncStrategy(){
		super(CONFIG_ENTITY_MANAGER, PAGE_MATCHERS);
	}
}
