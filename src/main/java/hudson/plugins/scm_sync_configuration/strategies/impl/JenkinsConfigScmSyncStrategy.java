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

import java.util.ArrayList;
import java.util.List;

public class JenkinsConfigScmSyncStrategy extends AbstractScmSyncStrategy {

	private static final List<PageMatcher> PAGE_MATCHERS = new ArrayList<PageMatcher>(){ {
        // Global configuration page
        add(new PageMatcher("^configure$", "form[name='config']"));
        // View configuration pages
        add(new PageMatcher("^(.+/)?view/[^/]+/configure$", "form[name='viewConfig']"));
        add(new PageMatcher("^newView$", "form[name='createView']"));
    } };
    
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
                return new WeightedMessage("Jenkins configuration files updated",
                        // Jenkins config update message should be considered as "important", especially
                        // more important than the plugin descriptors Saveable updates
                        MessageWeight.NORMAL);
            }
            public WeightedMessage getMessageWhenItemRenamed(Item item, String oldPath, String newPath) {
                throw new IllegalStateException("Jenkins configuration files should never be renamed !");
            }
            public WeightedMessage getMessageWhenItemDeleted(Item item) {
                throw new IllegalStateException("Jenkins configuration files should never be deleted !");
            }
        };
    }
}
