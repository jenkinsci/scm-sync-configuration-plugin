package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.plugins.scm_sync_configuration.JenkinsFilesHelper;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.model.ChangeSet;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;

@Extension
public class ScmSyncConfigurationSaveableListener extends SaveableListener{
		
	@Override
	public void onChange(Saveable o, XmlFile file) {
		
		super.onChange(o, file);
		
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
        if(plugin != null){
            ScmSyncStrategy strategy = plugin.getStrategyForSaveable(o, file.getFile());

            if(strategy != null){
                String path = JenkinsFilesHelper.buildPathRelativeToHudsonRoot(file.getFile());
                plugin.getTransaction().defineCommitMessage("Modification on configuration(s)", ChangeSet.MessageWeight.NORMAL);
                plugin.getTransaction().registerPath(path);
            }
        }
	}
}