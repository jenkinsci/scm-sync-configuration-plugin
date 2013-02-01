package hudson.plugins.test.utils.scms;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncHgSCM;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;

public class ScmUnderTestHg implements ScmUnderTest {

	public void initRepo(File path) throws Exception {
	    // HG support: Unlike Git, Mercurial does not appear to support "bare" repositories.
		// So we have to initialise the repository by committing a dummy file.

		final String dummyFileName = "hg_dummy.txt";
		File dummyFile = new File(path, dummyFileName);
		FileUtils.fileWrite(dummyFile.getAbsolutePath(), "This is a dummy file");

		ProcessBuilder[] commands = new ProcessBuilder[] {
			new ProcessBuilder("hg", "init"),
		    new ProcessBuilder("hg", "add", dummyFileName),
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
