package hudson.plugins.scm_sync_configuration.strategies.model;

import java.util.regex.Pattern;

public class PageMatcher {

	private Pattern urlRegex;
	private String targetFormSelector;
	
	public PageMatcher(String _urlRegexStr, String _targetFormSelector){
		this.urlRegex = Pattern.compile(_urlRegexStr);
		this.targetFormSelector = _targetFormSelector;
	}

	public Pattern getUrlRegex() {
		return urlRegex;
	}

	public String getTargetFormSelector() {
		return targetFormSelector;
	}
}
