package hudson.plugins.scm_sync_configuration.xstream;

import hudson.plugins.scm_sync_configuration.ScmSyncConfigurationPlugin;
import hudson.plugins.scm_sync_configuration.xstream.migration.AbstractMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.ScmSyncConfigurationDataMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.ScmSyncConfigurationPOJO;
import hudson.plugins.scm_sync_configuration.xstream.migration.v0.InitialMigrator;
import hudson.plugins.scm_sync_configuration.xstream.migration.v1.V0ToV1Migrator;

import java.util.logging.Logger;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter for ScmSyncConfigurationPlugin XStream data
 * Allows to provide API to migrate from one version to another of persisted scm sync configuration data
 * When creating a new migrator you must :
 * - Create a new package hudson.plugins.scm_sync_configuration.xstream.migration.v[X]
 * - Inside this package, copy/paste every classes located in hudson.plugins.scm_sync_configuration.xstream.migration.v[X-1]
 * - Rename every *V[X-1]* POJOs to *V[X]* POJO
 * - Eventually, change attributes in V[X]ScmSyncConfigurationPOJO (for example, if additionnal attribute has appeared)
 * - Provide implementation for V[X]Migrator.migrate() algorithm
 * - If parsing algorithm has changed, update V[X]Migrator.readScmSyncConfigurationPOJO with the new algorithm (if, for example, new root 
 * elements has appeared in XStream file)
 * - Update ScmSyncConfigurationXStreamConverter.MIGRATORS with new provided class
 * @author fcamblor
 */
public class ScmSyncConfigurationXStreamConverter implements Converter {
	
    private static final Logger LOGGER = Logger.getLogger(ScmSyncConfigurationXStreamConverter.class.getName());
    
    protected static final String VERSION_ATTRIBUTE = "version";
    
	/**
	 * Migrators for old versions of GlobalBuildStatsPlugin data representations
	 */
	private static final ScmSyncConfigurationDataMigrator[] MIGRATORS = new ScmSyncConfigurationDataMigrator[]{
		new InitialMigrator(),
		new V0ToV1Migrator()
	};

	/**
	 * Converter is only applicable on GlobalBuildStatsPlugin data
	 */
	public boolean canConvert(Class type) {
		return ScmSyncConfigurationPlugin.class.isAssignableFrom(type);
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		
		ScmSyncConfigurationPlugin plugin = (ScmSyncConfigurationPlugin)source;
		
		// Since "v1", providing version number in scm sync configuration heading tag
		writer.addAttribute(VERSION_ATTRIBUTE, String.valueOf(getCurrentScmSyncConfigurationVersionNumber()));
	
		if(plugin.getSCM() != null){
			writer.startNode(AbstractMigrator.SCM_TAG);
			writer.addAttribute(AbstractMigrator.SCM_CLASS_ATTRIBUTE, plugin.getSCM().getId());
			writer.endNode();
		}
		
		if(plugin.getScmRepositoryUrl() != null){
			writer.startNode(AbstractMigrator.SCM_REPOSITORY_URL_TAG);
			writer.setValue(plugin.getScmRepositoryUrl());
			writer.endNode();
		}

		writer.startNode(AbstractMigrator.SCM_NO_USER_COMMIT_MESSAGE);
		writer.setValue(Boolean.toString(plugin.isNoUserCommitMessage()));
		writer.endNode();

		writer.startNode(AbstractMigrator.SCM_DISPLAY_STATUS);
		writer.setValue(Boolean.toString(plugin.isDisplayStatus()));
		writer.endNode();

        if(plugin.getCommitMessagePattern() != null){
            writer.startNode(AbstractMigrator.SCM_COMMIT_MESSAGE_PATTERN);
            writer.setValue(plugin.getCommitMessagePattern());
            writer.endNode();
        }

        if(plugin.getManualSynchronizationIncludes() != null){
            writer.startNode(AbstractMigrator.SCM_MANUAL_INCLUDES);
            for(String include : plugin.getManualSynchronizationIncludes()){
                writer.startNode("include");
                writer.setValue(include);
                writer.endNode();
            }
            writer.endNode();
        }
    }
	
	/**
	 * @return current version number of scm sync configuration plugin
	 * data representation in XStream
	 */
	private static int getCurrentScmSyncConfigurationVersionNumber(){
		return MIGRATORS.length-1;
	}

	/**
	 * Will transform scm sync configuration XStream data representation into
	 * current ScmSyncConfigurationPlugin instance
	 */
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		
		ScmSyncConfigurationPlugin plugin;
		if(context.currentObject() == null || !(context.currentObject() instanceof ScmSyncConfigurationPlugin)){
			// This should never happen to get here
			plugin = new ScmSyncConfigurationPlugin();
		} else {
			// Retrieving already instantiated ScmSyncConfiguration plugin into current context ..
			plugin = (ScmSyncConfigurationPlugin)context.currentObject();
		}

		// Retrieving data representation version number
		String version = reader.getAttribute(VERSION_ATTRIBUTE);
		// Before version 1 (version 0), there wasn't any version in the scm sync configuration 
		// configuration file
		int versionNumber = 0;
		if(version != null){
			versionNumber = Integer.parseInt(version);
		}
		
		if(versionNumber != getCurrentScmSyncConfigurationVersionNumber()){
			// There will be a data migration ..
			LOGGER.info("Your version of persisted ScmSyncConfigurationPlugin data is not up-to-date (v"+versionNumber+" < v"+getCurrentScmSyncConfigurationVersionNumber()+") : data will be migrated !");
		}
		
		// Calling version's reader to read data representation
		ScmSyncConfigurationPOJO pojo = MIGRATORS[versionNumber].readScmSyncConfigurationPOJO(reader, context);
		
		// Migrating old data into up-to-date data
		// Added "+1" because we take into consideration InitialMigrator
		for(int i=versionNumber+1; i<getCurrentScmSyncConfigurationVersionNumber()+1; i++){
			pojo = MIGRATORS[i].migrate(pojo);
		}
		
		// Populating latest POJO information into ScmSyncConfigurationPlugin
		plugin.loadData(pojo);
		
		return plugin;
	}
}
