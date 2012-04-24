package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ClassAndFileConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class JobConfigScmSyncStrategy extends AbstractScmSyncStrategy {

    // Don't miss to take into account view urls since we can configure a job through a view !
	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ {
        add(new PageMatcher("^(.*view/[^/]+/)?job/[^/]+/configure$", "form[name='config']"));
    } };
    // Only saving config.xml file located in job directory
    // Some plugins (like maven release plugin) could add their own configuration files in the job directory that we don't want to synchronize
    // ... at least in the current strategy !
	private static final Pattern [] PATTERNS = new Pattern[] {
		Pattern.compile("^jobs/[^/]+/config\\.xml$")
	};
	private static final ConfigurationEntityMatcher CONFIG_ENTITY_MANAGER = new ClassAndFileConfigurationEntityMatcher(Job.class, PATTERNS);
	
	public JobConfigScmSyncStrategy(){
		super(CONFIG_ENTITY_MANAGER, PAGE_MATCHERS);
	}
	
	public List<File> createInitializationSynchronizedFileset() {
		List<File> syncedFiles = new ArrayList<File>();
		File hudsonJobsDirectory = new File(Hudson.getInstance().getRootDir().getAbsolutePath()+File.separator+"jobs");
		for(File hudsonJob : hudsonJobsDirectory.listFiles()){
			if(hudsonJob.isDirectory()){
				File hudsonJobConfig = new File(hudsonJob.getAbsoluteFile()+File.separator+"config.xml");
				syncedFiles.add(hudsonJobConfig);
			}
		}
		return syncedFiles;
	}
}
