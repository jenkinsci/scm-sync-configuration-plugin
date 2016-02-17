package hudson.plugins.scm_sync_configuration.scms;

import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.DescriptorImpl.Credential;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.logging.Level;

import org.kohsuke.stapler.StaplerRequest;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class ScmSyncSubversionSCM extends SCM {

    private static final String SCM_URL_PREFIX="scm:svn:";

    ScmSyncSubversionSCM(){
        super("Subversion", "svn/config.jelly", "hudson.scm.SubversionSCM", "/hudson/plugins/scm_sync_configuration/ScmSyncConfigurationPlugin/scms/svn/url-help.jelly");
    }

    @Override
    public String createScmUrlFromRequest(StaplerRequest req) {
        String repoURL = req.getParameter("repositoryUrl");
        if (repoURL == null) {
            return null;
        } else {
            return SCM_URL_PREFIX + repoURL.trim();
        }
    }

    @Override
    public String extractScmUrlFrom(String scmUrl) {
        return scmUrl.substring(SCM_URL_PREFIX.length());
    }

    @Override
    public SCMCredentialConfiguration extractScmCredentials(String scmUrl) {
        LOGGER.info("Extracting SVN Credentials for url : "+scmUrl);
        String realm = retrieveRealmFor(scmUrl);
        if(realm != null){
            LOGGER.fine("Extracted realm from "+scmUrl+" is ["+realm+"]");
            SubversionSCM.DescriptorImpl subversionDescriptor = (SubversionSCM.DescriptorImpl)getSCMDescriptor();
            try {
                Field credentialField = SubversionSCM.DescriptorImpl.class.getDeclaredField("credentials");
                credentialField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String,Credential> credentials = (Map<String,Credential>)credentialField.get(subversionDescriptor);
                Credential cred = credentials.get(realm);
                if(cred == null){
                    LOGGER.severe("No credentials are stored in Hudson for realm ["+realm+"] !");
                    return null;
                }
                String kind = ISVNAuthenticationManager.PASSWORD;
                if(scmUrl.startsWith("svn+ssh")){
                    kind = ISVNAuthenticationManager.SSH;
                }
                return createSCMCredentialConfiguration(cred.createSVNAuthentication(kind));
            } catch (SecurityException e) {
                LOGGER.log(Level.SEVERE, "'credentials' field not readable on SubversionSCM.DescriptorImpl !");
            } catch (NoSuchFieldException e) {
                LOGGER.log(Level.SEVERE, "'credentials' field not readable on SubversionSCM.DescriptorImpl !");
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "'credentials' field not accessible on "+String.valueOf(subversionDescriptor)+" !");
            } catch (IllegalAccessException e) {
                LOGGER.log(Level.SEVERE, "'credentials' field not accessible on "+String.valueOf(subversionDescriptor)+" !");
            } catch (SVNException e) {
                LOGGER.log(Level.WARNING, "Error creating SVN authentication from realm ["+realm+"] !", e);
            }
        } else {
            LOGGER.warning("No credential (realm) found for url ["+scmUrl+"] : it seems that you should enter your credentials in the UI at "
                    +"<a target='_blank' href='../../scm/SubversionSCM/enterCredential?"+scmUrl+"'>this url</a>");
        }
        return null;
    }

    private String retrieveRealmFor(String scmURL) {
        final String[] realms = new String[]{ null };

        SVNRepository repository;
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(scmURL));
            repository.setTunnelProvider(SVNWCUtil.createDefaultOptions(true));
        }catch(SVNException e){
            LOGGER.throwing(SVNRepositoryFactory.class.getName(), "create", e);
            LOGGER.severe("Error while creating SVNRepository : "+e.getMessage());
            return null;
        }
        try {
            repository.setAuthenticationManager(new DefaultSVNAuthenticationManager(SVNWCUtil.getDefaultConfigurationDirectory(), true, "", "", null, "") {
                @Override
                public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
                    realms[0] = realm;
                    return super.getFirstAuthentication(kind, realm, url);
                }
                @Override
                public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
                    realms[0] = realm;
                    return super.getNextAuthentication(kind, realm, url);
                }

                @Override
                public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
                    realms[0] = realm;
                    super.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
                }
            });
            repository.testConnection();

        } catch (SVNException e) {
            // If a problem happens, don't do anything, it implies realm doesn't exist in current cache
        }

        return realms[0];
    }

    /**
     * Ugly method to convert a SVN authentication into a SCMCredentialConfiguration
     */
    public SCMCredentialConfiguration createSCMCredentialConfiguration(SVNAuthentication auth){
        SCMCredentialConfiguration credentials = null;
        if(auth instanceof SVNPasswordAuthentication){
            SVNPasswordAuthentication passAuth = (SVNPasswordAuthentication)auth;
            credentials = new SCMCredentialConfiguration(passAuth.getUserName(), passAuth.getPassword());
        } else if(auth instanceof SVNSSHAuthentication){
            SVNSSHAuthentication sshAuth = (SVNSSHAuthentication)auth;
            credentials = new SCMCredentialConfiguration(sshAuth.getUserName(), sshAuth.getPassword(), sshAuth.getPassphrase(), sshAuth.getPrivateKey());
        } else if(auth instanceof SVNSSLAuthentication){
            SVNSSLAuthentication sslAuth = (SVNSSLAuthentication)auth;
            credentials = new SCMCredentialConfiguration(sslAuth.getUserName(), sslAuth.getPassword());
        } else if(auth instanceof SVNUserNameAuthentication){
            SVNUserNameAuthentication unameAuth = (SVNUserNameAuthentication)auth;
            credentials = new SCMCredentialConfiguration(unameAuth.getUserName());
        }

        if(credentials != null){
            LOGGER.info("Created SCM Credentials for user "+credentials.getUsername()+"...");
        }

        return credentials;
    }
}
