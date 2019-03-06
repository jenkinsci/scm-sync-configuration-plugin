package hudson.plugins.scm_sync_configuration.strategies.impl;

import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.Saveable;
import hudson.model.User;
import hudson.plugins.scm_sync_configuration.model.MessageWeight;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.AbstractScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.strategies.model.ClassAndFileConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.ConfigurationEntityMatcher;
import hudson.plugins.scm_sync_configuration.strategies.model.PageMatcher;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class UserConfigScmSyncStrategy extends AbstractScmSyncStrategy {

    // Don't miss to take into account view urls since we can configure a job through a view !
    private static final List<PageMatcher> PAGE_MATCHERS = ImmutableList.of(
            new PageMatcher("^securityRealm/addUser$", "#main-panel form"),
            new PageMatcher("^securityRealm/user/[^/]+/configure$", "form[name='config']")
            );

    // Only saving config.xml file located in user directory
    private static final String [] PATTERNS = new String[] {
        "users/*/config.xml"
    };

    private static final ConfigurationEntityMatcher CONFIG_ENTITY_MATCHER = new ClassAndFileConfigurationEntityMatcher(User.class, PATTERNS);

    public UserConfigScmSyncStrategy(){
        super(CONFIG_ENTITY_MATCHER, PAGE_MATCHERS);
    }

    @Override
    public CommitMessageFactory getCommitMessageFactory(){
        return new CommitMessageFactory(){
            @Override
            public WeightedMessage getMessageWhenSaveableUpdated(Saveable s, XmlFile file) {
                return new WeightedMessage("User ["+((User)s).getDisplayName()+"] configuration updated",
                        MessageWeight.IMPORTANT);
            }
            @Override
            public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath) {
                return new WeightedMessage("User ["+item.getName()+"] configuration renamed from ["+oldPath+"] to ["+newPath+"]",
                        MessageWeight.MORE_IMPORTANT);
            }
            @Override
            public WeightedMessage getMessageWhenItemDeleted(Item item) {
                return new WeightedMessage("User ["+item.getName()+"] hierarchy deleted",
                        MessageWeight.MORE_IMPORTANT);
            }
        };
    }

    @Override
    public boolean mightHaveBeenApplicableToDeletedSaveable(Saveable saveable, String pathRelativeToRoot, boolean wasDirectory) {
        return CONFIG_ENTITY_MATCHER.matches(saveable, pathRelativeToRoot, wasDirectory);
    }
}
