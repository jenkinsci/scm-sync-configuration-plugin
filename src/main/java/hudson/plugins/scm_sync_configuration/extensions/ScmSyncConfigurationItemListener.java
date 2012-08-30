package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.WeightedMessage;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;

import java.io.File;

@Extension
public class ScmSyncConfigurationItemListener extends ItemListener {

	@Override
	public void onLoaded() {
		super.onLoaded();
		
		// After every plugin is loaded, let's init ScmSyncConfigurationPlugin
		// Init is needed after plugin loads since it relies on scm implementations plugins loaded
        ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        if(plugin != null){
            plugin.init();
        }
	}
	
	@Override
	public void onDeleted(Item item) {
		super.onDeleted(item);
		
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        if(plugin != null){
            ScmSyncStrategy strategy = plugin.getStrategyForSaveable(item, null);

            if(strategy != null){
                WeightedMessage message = strategy.getCommitMessageFactory().getMessageWhenItemDeleted(item);
                plugin.getTransaction().defineCommitMessage(message);
                String path = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(item.getRootDir());
                plugin.getTransaction().registerPathForDeletion(path);
            }
        }
	}
	
	@Override
	public void onCreated(Item item) {
		super.onCreated(item);
	}
	
	@Override
	public void onCopied(Item src, Item item) {
		super.onCopied(src, item);
	}
	
	@Override
	public void onRenamed(Item item, String oldName, String newName) {
		super.onRenamed(item, oldName, newName);
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        if(plugin != null){
            ScmSyncStrategy strategy = plugin.getStrategyForSaveable(item, null);

            if(strategy != null){
                File parentDir = item.getRootDir().getParentFile();
                File oldDir = new File( parentDir.getAbsolutePath()+File.separator+oldName );
                File newDir = new File( parentDir.getAbsolutePath()+File.separator+newName );

                String oldPath = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(oldDir);
                String newPath = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(newDir);
                WeightedMessage message = strategy.getCommitMessageFactory().getMessageWhenItemRenamed(item, oldPath, newPath);
                plugin.getTransaction().defineCommitMessage(message);
                plugin.getTransaction().registerRenamedPath(oldPath, newPath);
            }
        }
	}
}
