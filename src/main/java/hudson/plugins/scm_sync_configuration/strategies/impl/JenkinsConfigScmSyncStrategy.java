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

import java.util.List;

import com.google.common.collect.ImmutableList;

public class JenkinsConfigScmSyncStrategy extends AbstractScmSyncStrategy {

	private static final List<PageMatcher> PAGE_MATCHERS = ImmutableList.of(
        // Global configuration page
        new PageMatcher("^configure$", "form[name='config']"),
        // View configuration pages
        new PageMatcher("^(.+/)?view/[^/]+/configure$", "form[name='viewConfig']"),
        new PageMatcher("^newView$", "form[name='createView'],form[name='createItem']")
    );
    
    private static final String[] PATTERNS = new String[]{
        "config.xml"
    };
    
	private static final ConfigurationEntityMatcher CONFIG_ENTITY_MATCHER = new PatternsEntityMatcher(PATTERNS);
	
	public JenkinsConfigScmSyncStrategy(){
		super(CONFIG_ENTITY_MATCHER, PAGE_MATCHERS);
	}

    public CommitMessageFactory getCommitMessageFactory(){
        return new CommitMessageFactory(){
            public WeightedMessage getMessageWhenSaveableUpdated(Saveable s, XmlFile file) {
                return new WeightedMessage("Jenkins configuration files updated", MessageWeight.NORMAL);
            }
            public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath) {
                throw new IllegalStateException("Jenkins configuration files should never be renamed !");
            }
            public WeightedMessage getMessageWhenItemDeleted(Item item) {
                throw new IllegalStateException("Jenkins configuration files should never be deleted !");
            }
        };
    }

	public boolean mightHaveBeenApplicableToDeletedSaveable(Saveable saveable, String pathRelativeToRoot, boolean wasDirectory) {
		// Uh-oh... Jenkins config should never be deleted.
		return false;
	}
}
