package hudson.plugins.scm_sync_configuration;

import hudson.plugins.scm_sync_configuration.model.BotherTimeout;

import org.kohsuke.stapler.Stapler;

import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class ScmSyncConfigurationDataProvider {

    private static final ThreadLocal<HttpServletRequest> CURRENT_REQUEST = new ThreadLocal<HttpServletRequest>();
	private static final String COMMENT_SESSION_KEY = "__commitMessage";
	private static final String BOTHER_TIMEOUTS_SESSION_KEY = "__botherTimeouts"; 
	
	public static void provideBotherTimeout(String type, int timeoutMinutesFromNow, String currentUrl){
		// FIXME: see if it wouldn't be possible to replace "currentURL" by "current file path"
		// in order to be able to target same files with different urls (jobs via views for example)
		
		Map<BotherTimeout, Date> botherTimeouts = retrievePurgedBotherTimeouts();
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
		currentRequest().getSession().setAttribute(BOTHER_TIMEOUTS_SESSION_KEY, botherTimeouts);
	}
	
	public static Date retrieveBotherTimeoutMatchingUrl(String currentURL){
		Map<BotherTimeout, Date> botherTimeouts = retrievePurgedBotherTimeouts();
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
	
	protected static Map<BotherTimeout, Date> retrievePurgedBotherTimeouts(){
		@SuppressWarnings("unchecked")
		Map<BotherTimeout, Date> botherTimeouts = (Map<BotherTimeout, Date>)retrieveObject(BOTHER_TIMEOUTS_SESSION_KEY, false);
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
	
	public static void provideComment(String comment){
		currentRequest().getSession().setAttribute(COMMENT_SESSION_KEY, comment);
	}
	
	public static String retrieveComment(boolean cleanComment){
		return (String)retrieveObject(COMMENT_SESSION_KEY, cleanComment);
	}
	
	private static Object retrieveObject(String key, boolean cleanObject){
        HttpServletRequest request = currentRequest();
        Object obj = null;
   		// Sometimes, request can be null : when hudson starts for instance !
        if(request != null){
            obj = request.getSession().getAttribute(key);
            if(cleanObject){
                request.getSession().removeAttribute(key);
            }
        }
		return obj;
	}

    public static void provideRequestDuring(HttpServletRequest request, Callable<Void> callable) throws Exception {
        CURRENT_REQUEST.set(request);

        try {
            callable.call();
        } finally {
            CURRENT_REQUEST.set(null);
        }
    }

    protected static HttpServletRequest currentRequest(){
        if(Stapler.getCurrentRequest() == null){
            return CURRENT_REQUEST.get();
        } else {
            return Stapler.getCurrentRequest();
        }
    }
}
