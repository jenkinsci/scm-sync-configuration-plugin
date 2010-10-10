package hudson.plugins.scm_sync_configuration.strategies.model;

import java.util.regex.Pattern;

public class PageMatcher {

	private Pattern urlRegex;
	private String targetFormName;
	
	public PageMatcher(String _urlRegexStr, String _targetFormName){
		this.urlRegex = Pattern.compile(_urlRegexStr);
		this.targetFormName = _targetFormName;
	}

	public Pattern getUrlRegex() {
		return urlRegex;
	}

	public String getTargetFormName() {
		return targetFormName;
	}
}
