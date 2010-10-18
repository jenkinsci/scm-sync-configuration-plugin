package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;

import java.util.logging.Logger;

@Extension
public class ScmSyncConfigurationSaveableListener extends SaveableListener{
	
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationSaveableListener.class.getName());

	public ScmSyncConfigurationSaveableListener(){
	}
	
	@Override
	public void onChange(Saveable o, XmlFile file) {
		
		super.onChange(o, file);
		
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		ScmSyncStrategy strategy = plugin.getStrategyForSaveable(o, file.getFile());
		
		if(strategy != null){
			plugin.commitFile(file);
		}
	}
}