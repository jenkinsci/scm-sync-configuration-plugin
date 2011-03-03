package hudson.plugins.scm_sync_configuration.xstream.migration;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * Behavior for ScmSyncConfiguration readers
 * @author fcamblor
 * @param <T>
 */
public interface ScmSyncConfigurationXStreamReader<T extends ScmSyncConfigurationPOJO> {
	T readScmSyncConfigurationPOJO(HierarchicalStreamReader reader, UnmarshallingContext context);
}
