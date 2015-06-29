package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.TopLevelItem;
import hudson.model.listeners.ItemListener;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;
import hudson.plugins.scm_sync_configuration.transactions.ScmTransaction;

import java.io.File;

import jenkins.model.DirectlyModifiableTopLevelItemGroup;
import jenkins.model.Jenkins;

@Extension
public class ScmSyncConfigurationItemListener extends ItemListener {

	@Override
	public void onLoaded() {
		super.onLoaded();
		
		// After every plugin is loaded, let's init ScmSyncConfigurationPlugin
		// Init is needed after plugin loads since it relies on scm implementations plugins loaded
        ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        if (plugin != null) {
            plugin.init();
        }
	}
	
	@Override
	public void onDeleted(Item item) {
		super.onDeleted(item);
		
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        if(plugin != null){
            String path = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(item.getRootDir());
            ScmSyncStrategy strategy = plugin.getStrategyForDeletedSaveable(item, path, true);
            if (strategy != null) {
                WeightedMessage message = strategy.getCommitMessageFactory().getMessageWhenItemDeleted(item);
        		ScmTransaction transaction = plugin.getTransaction();
                transaction.defineCommitMessage(message);
                transaction.registerPathForDeletion(path);
            }
        }
	}
	
	@Override
	public void onLocationChanged(Item item, String oldFullName, String newFullName) {
		super.onLocationChanged(item, oldFullName, newFullName);
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		if (plugin == null) {
			return;
		}
		// Figure out where the item previously might have been.
		File oldDir = null;
		Jenkins jenkins = Jenkins.getInstance();
		int i = oldFullName.lastIndexOf('/');
		String oldSimpleName = i > 0 ? oldFullName.substring(i+1) : oldFullName;
		Object oldParent = i > 0 ? jenkins.getItemByFullName(oldFullName.substring(0, i)) : jenkins;
		Object newParent = item.getParent();
		if (newParent == null) {
			// Shouldn't happen.
			newParent = jenkins;
		}
		if (oldParent == newParent && oldParent != null) {
			// Simple rename within the same directory
			oldDir = new File (item.getRootDir().getParentFile(), oldSimpleName);
		} else if (oldParent instanceof DirectlyModifiableTopLevelItemGroup && item instanceof TopLevelItem) {
			oldDir = ((DirectlyModifiableTopLevelItemGroup) oldParent).getRootDirFor((TopLevelItem) item);
			oldDir = new File (oldDir.getParentFile(), oldSimpleName);
		}
		ScmSyncStrategy oldStrategy = null;
		if (oldDir != null) {
			oldStrategy = plugin.getStrategyForDeletedSaveable(item, JenkinsFilesHelper.buildPathRelativeToHudsonRoot(oldDir), true);
		}
		File newDir = item.getRootDir();
		ScmSyncStrategy newStrategy = plugin.getStrategyForSaveable(item, newDir);
		ScmTransaction transaction = plugin.getTransaction();
		if (newStrategy == null) {
			if (oldStrategy != null) {
				// Delete old
                WeightedMessage message = oldStrategy.getCommitMessageFactory().getMessageWhenItemRenamed(item, oldFullName, newFullName);
                transaction.defineCommitMessage(message);
                transaction.registerPathForDeletion(JenkinsFilesHelper.buildPathRelativeToHudsonRoot(oldDir));
			}
		} else {
			String newPathRelativeToRoot = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(newDir);
			// Something moved to a place where we do cover it.
            WeightedMessage message = newStrategy.getCommitMessageFactory().getMessageWhenItemRenamed(item, oldFullName, newFullName);
            transaction.defineCommitMessage(message);
            transaction.registerRenamedPath(oldStrategy != null ? JenkinsFilesHelper.buildPathRelativeToHudsonRoot(oldDir) : null, newPathRelativeToRoot);
		}
	}
}
