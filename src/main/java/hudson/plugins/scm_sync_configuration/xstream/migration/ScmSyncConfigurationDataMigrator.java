package hudson.plugins.scm_sync_configuration.xstream.migration;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Migrator from old GlobalBuildStats POJO to later GlobalBuildStats POJO
 * @author fcamblor
 * @param <TFROM>
 * @param <TTO>
 */
public interface ScmSyncConfigurationDataMigrator<TFROM extends ScmSyncConfigurationPOJO, TTO extends ScmSyncConfigurationPOJO> {
	public TTO migrate(TFROM pojo);
	public TTO readScmSyncConfigurationPOJO(HierarchicalStreamReader reader, UnmarshallingContext context);	
}
