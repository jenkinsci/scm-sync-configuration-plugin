package hudson.plugins.scm_sync_configuration.model;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.builder.HashCodeBuilder;

public abstract class BotherTimeout {

	protected Date timeout;
	
	protected BotherTimeout(int _timeoutMinutesFromNow){
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, _timeoutMinutesFromNow);
		this.timeout = cal.getTime();
	}
	
	public boolean isOutdated(){
		return this.timeout.after(new Date());
	}
	
	public abstract boolean matchesUrl(String currentUrl);
	
	public static class FACTORY {
		public static BotherTimeout createBotherTimeout(String type, int timeoutMinutesFromNow, String currentUrl){
			if("thisConfig".equals(type)){
				return new CurrentConfig(timeoutMinutesFromNow, currentUrl);
			} else if("anyConfigs".equals(type)){
				return new EveryConfigs(timeoutMinutesFromNow);
			} else {
				throw new IllegalArgumentException("Invalid bother timeout type : "+String.valueOf(type));
			}
		}
	}
	
	public static class EveryConfigs extends BotherTimeout {
		protected EveryConfigs(int _timeoutMinutesFromNow){
			super(_timeoutMinutesFromNow);
		}
		public boolean matchesUrl(String currentUrl) {
			return true;
		}
		@Override
		public boolean equals(Object that) {
			if ( this == that ) return true;
			return that instanceof EveryConfigs;
		}
		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 23).toHashCode();
		}
	}
	
	public static class CurrentConfig extends BotherTimeout {
		private String url;
		public CurrentConfig(int _timeoutMinutesFromNow, String _url){
			super(_timeoutMinutesFromNow);
			this.url = _url;
		}
		public boolean matchesUrl(String currentUrl){
			if(currentUrl==null) return false;
			return this.url.equals(currentUrl);
		}
		@Override
		public boolean equals(Object that) {
			if ( this == that ) return true;
			if ( !(that instanceof CurrentConfig) ) return false;
			return url.equals(((CurrentConfig)that).url);
		}
		@Override
		public int hashCode() {
			return new HashCodeBuilder(13, 17).append(url).toHashCode();
		}
	}
}
