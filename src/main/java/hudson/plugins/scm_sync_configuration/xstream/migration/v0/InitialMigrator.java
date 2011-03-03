package hudson.plugins.scm_sync_configuration.xstream.migration.v0;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.impl.ScmSyncSubversionSCM;
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
	protected SCM createSCMFrom(String clazz, String content) {
		// v0.0.2 of the plugin was representing SCM as an enum type
		// so "class" attribute was not present here
		if(clazz == null){
			// And the only SCM implementation was the subversion one
			return new ScmSyncSubversionSCM();
		// In v0.0.3 there wasn't any "version" attribute but the
		// SCM was not represented as an enum type anymore .. so the "class" attribute
		// will be present and will be useful to determine the SCM implementation to chose
		} else {
			return SCM.valueOf(clazz);
		}
	}
	
}
