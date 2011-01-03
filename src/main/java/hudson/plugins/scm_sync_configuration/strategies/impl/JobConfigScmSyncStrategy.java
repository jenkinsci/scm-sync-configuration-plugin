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

public class JobConfigScmSyncStrategy extends AbstractScmSyncStrategy {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ { add(new PageMatcher("^(.*view/[^/]+/)?job/[^/]+/configure$", "config")); } };
	private static final ConfigurationEntityMatcher CONFIG_ENTITY_MANAGER = new ClassAndFileConfigurationEntityMatcher(Job.class, "^jobs/[^/]+/config\\.xml$");
	
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
