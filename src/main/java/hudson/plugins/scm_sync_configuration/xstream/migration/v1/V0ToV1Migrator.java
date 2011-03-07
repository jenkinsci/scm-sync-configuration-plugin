package hudson.plugins.scm_sync_configuration.xstream.migration.v1;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.scms.ScmSyncNoSCM;
import hudson.plugins.scm_sync_configuration.xstream.migration.AbstractMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.v0.V0ScmSyncConfigurationPOJO;

/**
 * V1 Evolutions :
 * - Apparition of tag "version" valued to "1" in scm-sync-configuration root tag
 * - SCM implementation package moved from hudson.plugins.scm_sync_configuration.scms.impl to hudson.plugins.scm_sync_configuration.scms
 * @author fcamblor
 */
public class V0ToV1Migrator extends AbstractMigrator<V0ScmSyncConfigurationPOJO, V1ScmSyncConfigurationPOJO> {

	@Override
	protected V1ScmSyncConfigurationPOJO createMigratedPojo() {
		return new V1ScmSyncConfigurationPOJO();
	}

	@Override
	protected SCM createSCMFrom(String classname, String content) {
		if(content == null){
			return SCM.valueOf(ScmSyncNoSCM.class);
		} else {
			return SCM.valueOf(classname);
		}
	}
}
