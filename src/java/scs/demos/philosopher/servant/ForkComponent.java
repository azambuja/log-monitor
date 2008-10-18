package scs.demos.philosopher.servant;

import java.util.ArrayList;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.servant.IComponentServant;
import scs.demos.philosopher.Fork;
import scs.demos.philosopher.ForkHelper;

public class ForkComponent extends IComponentServant {

	private ForkServant forkServant = null;
	private Fork fork = null;
	
	private final static String IFACE_FORK = "scs::demos::philosopher::Fork";
	private final static String FACET_FORK = "Fork";
	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.interface_name = IFACE_FORK;
		fd.name= FACET_FORK;
		fd.facet_ref = getFork();
		facets.add(fd);
		
		return facets;
	}

	private Fork getFork() {
		if( this.forkServant == null ) {
			try {
				this.forkServant = new ForkServant(); 
				this.fork = ForkHelper.narrow(this._poa().servant_to_reference(this.forkServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.fork;
	}
	@Override
	protected boolean doShutdown() {
		return true;
	}

	@Override
	protected boolean doStartup() {
		return true;
	}

}
