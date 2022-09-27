package hudson.plugins.test.utils.scms;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncSubversionSCM;

import java.io.File;

import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

public class ScmUnderTestSubversion implements ScmUnderTest {

	public void initRepo(File path) throws Exception {
		SVNRepositoryFactory.createLocalRepository(path, true , false);
	}

	public String createUrl(String url) {
		return "scm:svn:file://" + url.replace('\\', '/');
	}

	public Class<? extends SCM> getClazz() {
		return ScmSyncSubversionSCM.class;
	}

	public boolean useCredentials() {
		return true;
	}

	public String getSuffixForTestFiles() {
		return ".subversion";
	}

}
