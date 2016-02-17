package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PatternsEntityMatcher;

import java.util.Collections;
import java.util.List;

public class ManualIncludesScmSyncStrategy extends AbstractScmSyncStrategy {

    public ManualIncludesScmSyncStrategy(){
        super(null, Collections.<PageMatcher>emptyList());
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

    @Override
    public boolean mightHaveBeenApplicableToDeletedSaveable(Saveable saveable, String pathRelativeToRoot, boolean wasDirectory) {
        // Best we can do here. We'll double check later on in the transaction.
        return true;
    }
}
