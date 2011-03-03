package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

/**
 * Generic interface for ScmSyncConfiguration POJOs
 * @author fcamblor
 */
public interface ScmSyncConfigurationPOJO {
	public String getScmRepositoryUrl();
	public void setScmRepositoryUrl(String scmRepositoryUrl);
	public SCM getScm();
	public void setScm(SCM scm);
}
