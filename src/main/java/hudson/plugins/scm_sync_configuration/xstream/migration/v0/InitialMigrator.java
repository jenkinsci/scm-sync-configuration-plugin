package hudson.plugins.scm_sync_configuration.xstream.migration.v0;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncSubversionSCM;
import hudson.plugins.scm_sync_configuration.xstream.migration.AbstractMigrator;

/**
 * Initial representation of scm-sync-configuration.xml file
 * @author fcamblor
 */
public class InitialMigrator extends AbstractMigrator<V0ScmSyncConfigurationPOJO, V0ScmSyncConfigurationPOJO> {

	@Override
	protected V0ScmSyncConfigurationPOJO createMigratedPojo() {
		return new V0ScmSyncConfigurationPOJO();
	}
	
	@Override
	public V0ScmSyncConfigurationPOJO migrate(V0ScmSyncConfigurationPOJO pojo) {
		throw new IllegalAccessError("migrate() method should never be called on InitialMigrator !");
	}

	@Override
	protected SCM createSCMFrom(String classname, String content) {
		// No scm tag => no scm entered
		if(content == null){
			return SCM.valueOf(ScmSyncNoSCM.class);
		}
		
		// Starting from here, scm tag was provided...
		
		// v0.0.2 of the plugin was representing SCM as an enum type
		// so "class" attribute was not present here
		if(classname == null){
			// And the only SCM implementation in v0.0.2 was the subversion one
			return SCM.valueOf(ScmSyncSubversionSCM.class);
		// In v0.0.3 there wasn't any "version" attribute and the
		// SCM was not represented as an enum type anymore .. so the "class" attribute
		// will be present and will be useful to determine the SCM implementation to chose
		} else {
			// For backward compatibility
			if("hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncSubversionSCM".equals(classname)){
				classname = ScmSyncSubversionSCM.class.getName();
			} else if("hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncNoSCM".equals(classname)){
				classname = ScmSyncNoSCM.class.getName();
			}
			return SCM.valueOf(classname);
		}
	}
	
}
