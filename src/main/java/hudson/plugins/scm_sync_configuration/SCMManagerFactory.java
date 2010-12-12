package hudson.plugins.scm_sync_configuration;

import org.apache.maven.scm.manager.ScmManager;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;

public class SCMManagerFactory {

	private static final SCMManagerFactory INSTANCE = new SCMManagerFactory();
	
	private Embedder plexus;

	private SCMManagerFactory(){
	}
	
	public void start() throws Exception {
		this.plexus = new Embedder();
		this.plexus.start();
	}
	
	public void stop() throws Exception {
		this.plexus.stop();
	}
	
	public ScmManager createScmManager() throws ComponentLookupException {
		return (ScmManager)this.plexus.lookup(ScmManager.ROLE);
	}
	
	public static SCMManagerFactory getInstance(){
		return INSTANCE;
	}
}
