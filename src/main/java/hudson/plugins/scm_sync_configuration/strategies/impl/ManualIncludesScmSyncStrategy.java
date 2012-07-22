package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PatternsEntityMatcher;

import java.util.ArrayList;
import java.util.List;

public class ManualIncludesScmSyncStrategy extends AbstractScmSyncStrategy {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ {
        // No page matcher for this particular implementation
    } };

	public ManualIncludesScmSyncStrategy(){
		super(null, PAGE_MATCHERS);
	}

    @Override
    protected ConfigurationEntityMatcher createConfigEntityMatcher(){
        String[] includes = new String[0];
        List<String> manualSynchronizationIncludes = ScmSyncConfigurationPlugin.getInstance().getManualSynchronizationIncludes();
        if(manualSynchronizationIncludes != null){
            includes = manualSynchronizationIncludes.toArray(new String[0]);
        }
        return new PatternsEntityMatcher(includes);
    }
}
