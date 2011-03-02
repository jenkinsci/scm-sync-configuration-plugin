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
		        PlexusContainer.class.getDeclaredMethod("initialize").invoke(this.plexus);
		        PlexusContainer.class.getDeclaredMethod("start").invoke(this.plexus);
		    } catch (Throwable e) {}
			//this.plexus.start();
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
