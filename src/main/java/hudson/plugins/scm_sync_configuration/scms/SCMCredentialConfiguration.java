package hudson.plugins.scm_sync_configuration.scms;

public class SCMCredentialConfiguration {
    private String username;
    private String password;
    private String privateKey;
    private String passphrase;

    public SCMCredentialConfiguration(String _username, String _password, String _passPhrase, char[] _privateKey){
    	this.username = _username;
    	this.password = _password;
    	this.passphrase = _passPhrase;
    	if(_privateKey!=null){
    		this.privateKey = String.valueOf(_privateKey);
    	}
    }
    
    public SCMCredentialConfiguration(String _username, String _password){
    	this(_username, _password, null, null);
    }
    
    public SCMCredentialConfiguration(String _username){
    	this(_username, null, null, null);
    }
    
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public String getPassphrase() {
		return passphrase;
	}
}
