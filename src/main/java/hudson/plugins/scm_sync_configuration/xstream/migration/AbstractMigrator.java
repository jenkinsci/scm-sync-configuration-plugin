package hudson.plugins.scm_sync_configuration.xstream.migration;

import hudson.plugins.scm_sync_configuration.scms.SCM;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public abstract class AbstractMigrator<TFROM extends ScmSyncConfigurationPOJO, TTO extends ScmSyncConfigurationPOJO> implements ScmSyncConfigurationDataMigrator<TFROM, TTO> {
	
	public TTO migrate(TFROM pojo){
		TTO migratedPojo = createMigratedPojo();
		
		migratedPojo.setScmRepositoryUrl( migrateScmRepositoryUrl(pojo.getScmRepositoryUrl()) );
		migratedPojo.setScm( migrateScm(pojo.getScm()) );
		
		return migratedPojo;
	}
	
	public TTO readScmSyncConfigurationPOJO(
			HierarchicalStreamReader reader, UnmarshallingContext context) {
		
		TTO pojo = createMigratedPojo();
		
		reader.moveDown();
		String scmRepositoryUrl = reader.getValue();
		reader.moveUp();
		
		reader.moveDown();
		String scmClassAttribute = reader.getAttribute("class");
		String scmContent = reader.getValue();
		reader.moveUp();
		
		pojo.setScm(createSCMFrom(scmClassAttribute, scmContent));
		pojo.setScmRepositoryUrl(scmRepositoryUrl);
		
		return pojo;
	}
	
	// Overridable
	protected String migrateScmRepositoryUrl(String scmRepositoryUrl){
		return new String(scmRepositoryUrl);
	}
	
	// Overridable
	protected SCM migrateScm(SCM scm){
		return SCM.valueOf(scm.getClass().getName());
	}
	
	protected abstract TTO createMigratedPojo();
	protected abstract SCM createSCMFrom(String clazz, String content);
}
