package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Job;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.util.ArrayList;
import java.util.List;

public class JobConfigScmSyncStrategy extends AbstractScmSyncStrategy<Job> {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ { add(new PageMatcher("^job/[^/]+/configure$", "config")); } };
	
	public JobConfigScmSyncStrategy(){
		super(Job.class, PAGE_MATCHERS);
	}
	
}
