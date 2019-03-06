package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.plugins.scm_sync_configuration.model.MessageWeight;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PatternsEntityMatcher;

import java.util.Collections;

public class BasicPluginsConfigScmSyncStrategy extends AbstractScmSyncStrategy {

    private static final String[] PATTERNS = new String[]{
        "hudson*.xml",
        "jenkins*.xml",
        "scm-sync-configuration.xml"
    };

    private static final ConfigurationEntityMatcher CONFIG_ENTITY_MATCHER = new PatternsEntityMatcher(PATTERNS);

    public BasicPluginsConfigScmSyncStrategy(){
        super(CONFIG_ENTITY_MATCHER, Collections.<PageMatcher>emptyList());
    }

    @Override
    public CommitMessageFactory getCommitMessageFactory(){
        return new CommitMessageFactory(){
            @Override
            public WeightedMessage getMessageWhenSaveableUpdated(Saveable s, XmlFile file) {
                return new WeightedMessage("Plugin configuration files updated", MessageWeight.MINIMAL);
            }
            @Override
            public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath) {
                // It should never happen... but who cares how will behave *every* plugin in the jenkins land ?
                return new WeightedMessage("Plugin configuration files renamed", MessageWeight.MINIMAL);
            }
            @Override
            public WeightedMessage getMessageWhenItemDeleted(Item item) {
                // It should never happen... but who cares how will behave *every* plugin in the jenkins land ?
                return new WeightedMessage("Plugin configuration files deleted", MessageWeight.MINIMAL);
            }
        };
    }

    @Override
    public boolean mightHaveBeenApplicableToDeletedSaveable(Saveable saveable, String pathRelativeToRoot, boolean wasDirectory) {
        return !wasDirectory && pathRelativeToRoot != null && CONFIG_ENTITY_MATCHER.matches(saveable,  pathRelativeToRoot, false);
    }
}
