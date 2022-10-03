package hudson.plugins.test.utils.scms;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncGitSCM;

import java.io.File;

public class ScmUnderTestGit implements ScmUnderTest {

	public void initRepo(File path) throws Exception {
		ProcessBuilder pb = new ProcessBuilder("git", "init", "--bare");
		pb.directory(path);
		Process p = pb.start();
		if (p.waitFor() != 0) {
			throw new Exception("Unable to init git repo in " + path.getAbsolutePath());
		}
	}

	public String createUrl(String url) {
		return "scm:git:file:///" + url.replace('\\', '/');
	}

	public Class<? extends SCM> getClazz() {
		return ScmSyncGitSCM.class;
	}

	public boolean useCredentials() {
		return false;
	}

	public String getSuffixForTestFiles() {
		return ".git";
	}

}
