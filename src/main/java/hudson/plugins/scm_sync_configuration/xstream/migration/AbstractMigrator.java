package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public abstract class AbstractMigrator<TFROM extends ScmSyncConfigurationPOJO, TTO extends ScmSyncConfigurationPOJO> implements ScmSyncConfigurationDataMigrator<TFROM, TTO> {
	
	public static final String SCM_REPOSITORY_URL_TAG = "scmRepositoryUrl";
	public static final String SCM_TAG = "scm";
	public static final String SCM_CLASS_ATTRIBUTE = "class";
	public static final String SCM_NO_USER_COMMIT_MESSAGE = "noUserCommitMessage";
    public static final String SCM_DISPLAY_STATUS = "displayStatus";
    public static final String SCM_SYNC_JENKINS_CONFIG = "syncJenkinsConfig";
    public static final String SCM_SYNC_PLUGINS_CONFIG = "syncBasicPluginsConfig";
    public static final String SCM_SYNC_JOB_CONFIG = "syncJobConfig";
    public static final String SCM_SYNC_USER_DATA = "syncUserConfig";
    public static final String SCM_COMMIT_MESSAGE_PATTERN = "commitMessagePattern";
    public static final String SCM_MANUAL_INCLUDES = "manualSynchronizationIncludes";

    private static final Logger LOGGER = Logger.getLogger(AbstractMigrator.class.getName());

	public TTO migrate(TFROM pojo){
		TTO migratedPojo = createMigratedPojo();
		
		migratedPojo.setScmRepositoryUrl( migrateScmRepositoryUrl(pojo.getScmRepositoryUrl()) );
		migratedPojo.setScm( migrateScm(pojo.getScm()) );
		
		return migratedPojo;
	}
	
	public TTO readScmSyncConfigurationPOJO(
			HierarchicalStreamReader reader, UnmarshallingContext context) {
		
		TTO pojo = createMigratedPojo();
		
		String scmRepositoryUrl = null;
		String scmClassAttribute = null;
		String scmContent = null;
		boolean noUserCommitMessage = false;
		boolean displayStatus = true;
		boolean syncJenkinsConfig = true;
		boolean syncBasicPluginsConfig = true;
		boolean syncJobConfig = true;
		boolean syncUserConfig = true;
        String commitMessagePattern = "[message]";
        List<String> manualIncludes = null;
		
		while(reader.hasMoreChildren()){
			reader.moveDown();
			if(SCM_REPOSITORY_URL_TAG.equals(reader.getNodeName())){
				scmRepositoryUrl = reader.getValue();
			} else if(SCM_NO_USER_COMMIT_MESSAGE.equals(reader.getNodeName())){
				noUserCommitMessage = Boolean.parseBoolean(reader.getValue());
			} else if(SCM_DISPLAY_STATUS.equals(reader.getNodeName())){
				displayStatus = Boolean.parseBoolean(reader.getValue());
			} else if(SCM_SYNC_JENKINS_CONFIG.equals(reader.getNodeName())){
				syncJenkinsConfig = Boolean.parseBoolean(reader.getValue());
			} else if(SCM_SYNC_PLUGINS_CONFIG.equals(reader.getNodeName())){
				syncBasicPluginsConfig = Boolean.parseBoolean(reader.getValue());
			} else if(SCM_SYNC_JOB_CONFIG.equals(reader.getNodeName())){
				syncJobConfig = Boolean.parseBoolean(reader.getValue());
			} else if(SCM_SYNC_USER_DATA.equals(reader.getNodeName())){
				syncUserConfig = Boolean.parseBoolean(reader.getValue());
			} else if(SCM_TAG.equals(reader.getNodeName())){
				scmClassAttribute = reader.getAttribute(SCM_CLASS_ATTRIBUTE);
				scmContent = reader.getValue();
            } else if(SCM_COMMIT_MESSAGE_PATTERN.equals(reader.getNodeName())){
                commitMessagePattern = reader.getValue();
            } else if(SCM_MANUAL_INCLUDES.equals(reader.getNodeName())){
                manualIncludes = new ArrayList<String>();
                while(reader.hasMoreChildren()){
                    reader.moveDown();
                    manualIncludes.add(reader.getValue());
                    reader.moveUp();
                }
			} else {
				IllegalArgumentException iae = new IllegalArgumentException("Unknown tag : "+reader.getNodeName());
				LOGGER.throwing(this.getClass().getName(), "readScmSyncConfigurationPOJO", iae);
				LOGGER.severe("Unknown tag : "+reader.getNodeName());
				throw iae;
			}
			reader.moveUp();
		}
		
		pojo.setScm(createSCMFrom(scmClassAttribute, scmContent));
		pojo.setScmRepositoryUrl(scmRepositoryUrl);
		pojo.setNoUserCommitMessage(noUserCommitMessage);
		pojo.setDisplayStatus(displayStatus);
		pojo.setSyncJenkinsConfig(syncJenkinsConfig);
		pojo.setSyncBasicPluginsConfig(syncBasicPluginsConfig);
		pojo.setSyncJobConfig(syncJobConfig);
		pojo.setSyncUserConfig(syncUserConfig);
        pojo.setCommitMessagePattern(commitMessagePattern);
        pojo.setManualSynchronizationIncludes(manualIncludes);
		
		return pojo;
	}
	
	// Overridable
	protected String migrateScmRepositoryUrl(String scmRepositoryUrl){
		if(scmRepositoryUrl == null){
			return null;
		} else {
			return new String(scmRepositoryUrl);
		}
	}
	
	// Overridable
	protected SCM migrateScm(SCM scm){
		if(scm == null){
			return null;
		} else {
			return SCM.valueOf(scm.getClass().getName());
		}
	}
	
	protected abstract TTO createMigratedPojo();
	protected abstract SCM createSCMFrom(String clazz, String content);
}
