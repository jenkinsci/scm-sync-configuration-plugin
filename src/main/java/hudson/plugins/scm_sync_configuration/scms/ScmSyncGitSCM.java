package hudson.plugins.scm_sync_configuration.scms;

import org.kohsuke.stapler.StaplerRequest;

public class ScmSyncGitSCM extends SCM {

	private static final String SCM_URL_PREFIX="scm:git:";
	
	ScmSyncGitSCM(){
		super("Git", "git/config.jelly", "hudson.plugins.git.GitSCM", "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/scms/git/url-help.jelly");
	}
	
	public String createScmUrlFromRequest(StaplerRequest req) {
		String repoURL = req.getParameter("gitRepositoryUrl");
		if (repoURL == null) {
			return null;
		} else {
			return SCM_URL_PREFIX + repoURL.trim();
		}
	}
	
	public String extractScmUrlFrom(String scmUrl) {
		return scmUrl.substring(SCM_URL_PREFIX.length());
	}
	
	public SCMCredentialConfiguration extractScmCredentials(String scmUrl) {
		return null;
	}
	
}
