package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import java.util.logging.Logger;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public abstract class AbstractMigrator<TFROM extends ScmSyncConfigurationPOJO, TTO extends ScmSyncConfigurationPOJO> implements ScmSyncConfigurationDataMigrator<TFROM, TTO> {
	
	public static final String SCM_REPOSITORY_URL_TAG = "scmRepositoryUrl";
	public static final String SCM_COMMENT_PREFIX_TAG = "scmCommentPrefix";
	public static final String SCM_COMMENT_SUFFIX_TAG = "scmCommentSuffix";
	public static final String SCM_TAG = "scm";
	public static final String SCM_CLASS_ATTRIBUTE = "class";
    private static final Logger LOGGER = Logger.getLogger(AbstractMigrator.class.getName());

	public TTO migrate(TFROM pojo){
		TTO migratedPojo = createMigratedPojo();
		
		migratedPojo.setScmRepositoryUrl( migrateScmRepositoryUrl(pojo.getScmRepositoryUrl()) );
		migratedPojo.setScmCommentPrefix( migrateScmCommentPrefix(pojo.getScmCommentPrefix()) );
		migratedPojo.setScmCommentSuffix( migrateScmCommentSuffix(pojo.getScmCommentSuffix()) );
		migratedPojo.setScm( migrateScm(pojo.getScm()) );
		
		return migratedPojo;
	}
	
	public TTO readScmSyncConfigurationPOJO(
			HierarchicalStreamReader reader, UnmarshallingContext context) {
		
		TTO pojo = createMigratedPojo();
		
		String scmRepositoryUrl = null;
		String scmCommentPrefix = null;
		String scmCommentSuffix = null;
		String scmClassAttribute = null;
		String scmContent = null;
		while(reader.hasMoreChildren()){
			reader.moveDown();
			if(SCM_REPOSITORY_URL_TAG.equals(reader.getNodeName())){
				scmRepositoryUrl = reader.getValue();
			} else if(SCM_COMMENT_PREFIX_TAG.equals(reader.getNodeName())){
				scmCommentPrefix = reader.getValue();
			} else if(SCM_COMMENT_SUFFIX_TAG.equals(reader.getNodeName())){
				scmCommentSuffix = reader.getValue();
			} else if(SCM_TAG.equals(reader.getNodeName())){
				scmClassAttribute = reader.getAttribute(SCM_CLASS_ATTRIBUTE);
				scmContent = reader.getValue();
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
		pojo.setScmCommentPrefix(scmCommentPrefix);
		pojo.setScmCommentSuffix(scmCommentSuffix);
		
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
	protected String migrateScmCommentPrefix(String scmCommentPrefix){
		if(scmCommentPrefix == null){
			return null;
		} else {
			return new String(scmCommentPrefix);
		}
	}
	
	// Overridable
	protected String migrateScmCommentSuffix(String scmCommentSuffix){
		if(scmCommentSuffix == null){
			return null;
		} else {
			return new String(scmCommentSuffix);
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
