package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.model.listeners.SaveableListener;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.strategies.ScmSyncStrategy;

import java.io.File;

import org.acegisecurity.AccessDeniedException;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class ScmSyncConfigurationSaveableListener extends SaveableListener{
	
	public ScmSyncConfigurationSaveableListener(){
	}
	
	@Override
	public void onChange(Saveable o, XmlFile file) {
		
		super.onChange(o, file);
		
		ScmSyncConfigurationPlugin plugin = ScmSyncConfigurationPlugin.getInstance();
		ScmSyncStrategy strategy = plugin.getStrategyForSaveable(o);
		if(strategy != null){
			User user = null;
			try {
				user = Hudson.getInstance().getMe();
			}catch(AccessDeniedException e){}
			
			StaplerRequest req = Stapler.getCurrentRequest();
			// Sometimes, request can be null : when hudson starts for example !
			String comment = null;
			if(req != null){
				comment = (String)req.getSession().getAttribute("commitMessage");
			}
			
			plugin.commitFile(file, comment, user);
		}
	}
}