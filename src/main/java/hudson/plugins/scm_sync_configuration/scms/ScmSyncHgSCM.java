package hudson.plugins.scm_sync_configuration.scms;

import org.kohsuke.stapler.StaplerRequest;

public class ScmSyncHgSCM extends SCM {

	private static final String SCM_URL_PREFIX="scm:hg:";

	ScmSyncHgSCM(){
		super("Mercurial", "hg/config.jelly", "hudson.plugins.mercurial.MercurialSCM", "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/scms/hg/url-help.jelly");
	}

	public String createScmUrlFromRequest(StaplerRequest req) {
		String repoURL = req.getParameter("hgRepositoryUrl");
		if(repoURL == null){
			return null;
		}
		else {
			return SCM_URL_PREFIX+repoURL;
		}
	}

	public String extractScmUrlFrom(String scmUrl) {
		return scmUrl.substring(SCM_URL_PREFIX.length());
	}

	public SCMCredentialConfiguration extractScmCredentials(String scmUrl) {
		return null;
	}

}
