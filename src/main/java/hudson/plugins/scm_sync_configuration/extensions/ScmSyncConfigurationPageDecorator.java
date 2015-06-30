package hudson.plugins.scm_sync_configuration.extensions;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;

import org.kohsuke.stapler.bind.JavaScriptMethod;

@Extension
public class ScmSyncConfigurationPageDecorator extends PageDecorator{

	@SuppressWarnings("deprecation") // Super constructor is deprecated. Unsure if default constructor would work, though.
	public ScmSyncConfigurationPageDecorator(){
		super(ScmSyncConfigurationPageDecorator.class);
	}
	
	public ScmSyncConfigurationPlugin getScmSyncConfigPlugin(){
		return ScmSyncConfigurationPlugin.getInstance();
	}

    @JavaScriptMethod
   	public void purgeScmSyncConfigLogs() {
        getScmSyncConfigPlugin().purgeFailLogs();
   	}
}