package hudson.plugins.scm_sync_configuration;

import org.kohsuke.stapler.StaplerRequest;

public class ScmSyncConfigurationDataProvider {

	private static final String COMMENT_SESSION_KEY = "commitMessage";
	
	public static void provideComment(StaplerRequest request, String comment){
		request.getSession().setAttribute(COMMENT_SESSION_KEY, comment);
	}
	
	public static String retrieveComment(StaplerRequest request, boolean cleanComment){
		return (String)retrieveObject(request, COMMENT_SESSION_KEY, cleanComment);
	}
	
	private static Object retrieveObject(StaplerRequest request, String key, boolean cleanObject){
		Object obj = request.getSession().getAttribute(key);
		if(cleanObject){
			request.getSession().removeAttribute(key);
		}
		return obj;
	}
}
