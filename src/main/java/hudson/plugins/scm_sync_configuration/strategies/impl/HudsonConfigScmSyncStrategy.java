package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Hudson;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HudsonConfigScmSyncStrategy extends AbstractScmSyncStrategy<Hudson> {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ { add(new PageMatcher("^configure$", "config")); } };
	
	public HudsonConfigScmSyncStrategy(){
		super(Hudson.class, PAGE_MATCHERS);
	}
	
	public List<File> createInitializationSynchronizedFileset() {
		return new ArrayList<File>(){{ 
			add(new File(Hudson.getInstance().getRootDir().getAbsolutePath()+File.separator+"config.xml")); 
			}};
	}
}
