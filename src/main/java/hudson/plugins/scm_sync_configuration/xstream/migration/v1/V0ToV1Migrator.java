package hudson.plugins.scm_sync_configuration.xstream.migration.v1;

import hudson.plugins.scm_sync_configuration.scms.SCM;
import hudson.plugins.scm_sync_configuration.xstream.migration.AbstractMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.v0.V0ScmSyncConfigurationPOJO;

/**
 * V1 Evolutions :
 * - SCM implementation was provided in class scm tag attribute (instead of scm content)
 * @author fcamblor
 */
public class V0ToV1Migrator extends AbstractMigrator<V0ScmSyncConfigurationPOJO, V1ScmSyncConfigurationPOJO> {

	@Override
	protected V1ScmSyncConfigurationPOJO createMigratedPojo() {
		return new V1ScmSyncConfigurationPOJO();
	}

	@Override
	protected SCM createSCMFrom(String clazz, String content) {
		return SCM.valueOf(clazz);
	}
}
