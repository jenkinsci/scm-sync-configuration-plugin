package hudson.plugins.test.utils.scms;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.io.File;

public interface ScmUnderTest {

	void initRepo(File path) throws Exception;
	
	String createUrl(String url);
	
	Class<? extends SCM> getClazz();
	
	boolean useCredentials();

	String getSuffixForTestFiles();
	
}
