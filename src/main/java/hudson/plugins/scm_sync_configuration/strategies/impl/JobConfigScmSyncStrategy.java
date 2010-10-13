package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Hudson;
import hudson.model.Job;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;

public class JobConfigScmSyncStrategy extends AbstractScmSyncStrategy<Job> {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ { add(new PageMatcher("^job/[^/]+/configure$", "config")); } };
	
	public JobConfigScmSyncStrategy(){
		super(Job.class, PAGE_MATCHERS);
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
