package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.model.MessageWeight;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.JobOrFolderConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.util.Collections;

public class JobConfigScmSyncStrategy extends AbstractScmSyncStrategy {

	private static final ConfigurationEntityMatcher CONFIG_MATCHER = new JobOrFolderConfigurationEntityMatcher();
	
    public JobConfigScmSyncStrategy(){
		super(CONFIG_MATCHER, Collections.singletonList(new PageMatcher("^(.*view/[^/]+/)?(job/[^/]+/)+configure$", "form[name='config']")));
	}

    public CommitMessageFactory getCommitMessageFactory(){
        return new CommitMessageFactory(){
            public WeightedMessage getMessageWhenSaveableUpdated(Saveable s, XmlFile file) {
                return new WeightedMessage("Job ["+((Item)s).getName()+"] configuration updated",
                        MessageWeight.IMPORTANT);
            }
            public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath) {
                return new WeightedMessage("Job ["+item.getName()+"] hierarchy renamed from ["+oldPath+"] to ["+newPath+"]",
                        MessageWeight.MORE_IMPORTANT);
            }
            public WeightedMessage getMessageWhenItemDeleted(Item item) {
                return new WeightedMessage("Job ["+item.getName()+"] hierarchy deleted",
                        MessageWeight.MORE_IMPORTANT);
            }
        };
    }

	public boolean mightHaveBeenApplicableToDeletedSaveable(Saveable saveable, String pathRelativeFromRoot, boolean wasDirectory) {
		return CONFIG_MATCHER.matches(saveable, pathRelativeFromRoot, wasDirectory);
	}
}
