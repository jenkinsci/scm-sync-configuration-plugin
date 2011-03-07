package hudson.plugins.scm_sync_configuration.scms;

import org.kohsuke.stapler.StaplerRequest;


public class ScmSyncNoSCM extends SCM {

	ScmSyncNoSCM(){
		super("None", "none/config.jelly", null, "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/scms/none/url-help.jelly");
	}
	
	@Override
	public String createScmUrlFromRequest(StaplerRequest req) {
		return null;
	}

	@Override
	public String extractScmUrlFrom(String scmUrl) {
		return null;
	}

	@Override
	public SCMCredentialConfiguration extractScmCredentials(
			String scmRepositoryURL) {
		return null;
	}

}
