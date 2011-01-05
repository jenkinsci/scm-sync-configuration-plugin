package hudson.plugins.scm_sync_configuration;

import hudson.plugins.scm_sync_configuration.model.BotherTimeout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.stapler.StaplerRequest;

public class ScmSyncConfigurationDataProvider {

	private static final String COMMENT_SESSION_KEY = "__commitMessage";
	private static final String BOTHER_TIMEOUTS_SESSION_KEY = "__botherTimeouts"; 
	
	public static void provideBotherTimeout(StaplerRequest request, String type, int timeoutMinutesFromNow, String currentUrl){
		// FIXME: see if it wouldn't be possible to replace "currentURL" by "current file path"
		// in order to be able to target same files with different urls (jobs via views for example)
		
		Map<BotherTimeout, Date> botherTimeouts = retrievePurgedBotherTimeouts(request);
		if(botherTimeouts == null){
			botherTimeouts = new HashMap<BotherTimeout, Date>();
		}
		
		// Removing existing BotherTimeouts matching current url
		List< Entry<BotherTimeout, Date> > entriesToDelete = new ArrayList< Entry<BotherTimeout, Date> >();
		for(Entry<BotherTimeout, Date> entry : botherTimeouts.entrySet()){
			if(entry.getKey().matchesUrl(currentUrl)){
				entriesToDelete.add(entry);
			}
		}
		botherTimeouts.keySet().removeAll(entriesToDelete);
		
		// Adding new BotherTimeout with updated timeout
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, timeoutMinutesFromNow);
		BotherTimeout bt = BotherTimeout.FACTORY.createBotherTimeout(type, timeoutMinutesFromNow, currentUrl);
		botherTimeouts.put(bt, cal.getTime());
		
		// Updating session
		request.getSession().setAttribute(BOTHER_TIMEOUTS_SESSION_KEY, botherTimeouts);
	}
	
	public static Date retrieveBotherTimeoutMatchingUrl(StaplerRequest request, String currentURL){
		Map<BotherTimeout, Date> botherTimeouts = retrievePurgedBotherTimeouts(request);
		Date timeoutMatchingUrl = null;
		if(botherTimeouts != null){
			for(Entry<BotherTimeout, Date> entry : botherTimeouts.entrySet()){
				if(entry.getKey().matchesUrl(currentURL)){
					timeoutMatchingUrl = entry.getValue();
					break;
				}
			}
		}
		return timeoutMatchingUrl;
	}
	
	protected static Map<BotherTimeout, Date> retrievePurgedBotherTimeouts(StaplerRequest request){
		Map<BotherTimeout, Date> botherTimeouts = (Map<BotherTimeout, Date>)retrieveObject(request, BOTHER_TIMEOUTS_SESSION_KEY, false);
		if(botherTimeouts != null){
			purgeOutdatedBotherTimeouts(botherTimeouts);
		}
		return botherTimeouts;
	}
	
	protected static void purgeOutdatedBotherTimeouts(Map<BotherTimeout, Date> botherTimeouts){
		Date now = Calendar.getInstance().getTime();
		List< Entry<BotherTimeout, Date> > entriesToDelete = new ArrayList< Entry<BotherTimeout, Date> >();
		for(Entry<BotherTimeout, Date> entry : botherTimeouts.entrySet()){
			if(entry.getValue().before(now)){
				entriesToDelete.add(entry);
			}
		}
		botherTimeouts.entrySet().removeAll(entriesToDelete);
	}
	
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
