package hudson.plugins.test.utils.scms;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncHgSCM;

import java.io.File;

public class ScmUnderTestHg implements ScmUnderTest {

	public void initRepo(File path) throws Exception {
		ProcessBuilder[] commands = new ProcessBuilder[] {
		    // HG support: Unlike Git, Hg does not appear to support "bare" repos.
			// So we have to initialise the repo by commiting a dummy file.
			new ProcessBuilder("hg", "init"),
		    new ProcessBuilder("touch", "hg_dummy.txt"),
		    new ProcessBuilder("hg", "add", "hg_dummy.txt"),
		    new ProcessBuilder("hg", "commit", "-m", "\"initial commit\"")
		};
		for (ProcessBuilder pb : commands) {
			pb.directory(path);
			Process p = pb.start();
			if (p.waitFor() != 0) {
				throw new Exception("Unable to init hg repo in " + path.getAbsolutePath());
			}
		}
	}

	public String createUrl(String url) {
		return "scm:hg:" + url;
	}

	public Class<? extends SCM> getClazz() {
		return ScmSyncHgSCM.class;
	}

	public boolean useCredentials() {
		return false;
	}

	public String getSuffixForTestFiles() {
		return ".hg";
	}

}
