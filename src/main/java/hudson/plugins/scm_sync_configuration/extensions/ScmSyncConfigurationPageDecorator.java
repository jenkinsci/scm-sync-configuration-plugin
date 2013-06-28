package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationStatusManager;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@Extension
public class ScmSyncConfigurationPageDecorator extends PageDecorator{

	public ScmSyncConfigurationPageDecorator(){
		super(ScmSyncConfigurationPageDecorator.class);
	}

	public ScmSyncConfigurationPlugin getScmSyncConfigPlugin(){
		return ScmSyncConfigurationPlugin.getInstance();
	}

	@JavaScriptMethod
	public void RemoveLog() {
		ScmSyncConfigurationStatusManager manager = new ScmSyncConfigurationStatusManager();
		manager.getFailFile().delete();
	}
}
