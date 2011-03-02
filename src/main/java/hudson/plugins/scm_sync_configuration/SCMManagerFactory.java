package hudson.plugins.scm_sync_configuration;

import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class SCMManagerFactory {

	private static final SCMManagerFactory INSTANCE = new SCMManagerFactory();
	
	private PlexusContainer plexus = null;

	private SCMManagerFactory(){
	}
	
	public void start() throws PlexusContainerException {
		if(plexus == null){
			this.plexus = new DefaultPlexusContainer();
			try {
				// These will only be useful for Hudson v1.395 and under
				// ... Since the use of sisu-plexus-inject will initialize
				// everything in the constructor
		        PlexusContainer.class.getDeclaredMethod("initialize").invoke(this.plexus);
		        PlexusContainer.class.getDeclaredMethod("start").invoke(this.plexus);
		    } catch (Throwable e) { /* Don't do anything here ... initialize/start methods should be called prior to v1.395 ! */ }
		}
	}
	
	public ScmManager createScmManager() throws ComponentLookupException {
		return (ScmManager)this.plexus.lookup(ScmManager.ROLE);
	}
	
	public void stop() throws Exception {
		this.plexus.dispose();
		this.plexus = null;
	}
	
	public static SCMManagerFactory getInstance(){
		return INSTANCE;
	}
}
