package scs.demos.philosopher.servant;

import java.util.ArrayList;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.servant.IComponentServant;
import scs.demos.philosopher.Observer;
import scs.demos.philosopher.ObserverHelper;

public class ObserverComponent extends IComponentServant {

	private ObserverServant obsServant = null;
	private Observer observer = null;
	
	private final static String IFACE_OBSERVER = "scs::demos::philosopher::Observer";
	private final static String FACET_OBSERVER = "Observer";

	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.interface_name = IFACE_OBSERVER;
		fd.name= FACET_OBSERVER;
		fd.facet_ref = getObserver();
		facets.add(fd);
		
		return facets;
	}
	
	private Observer getObserver() {
		if( this.obsServant == null ) {
			try {
				this.obsServant = new ObserverServant(); 
				this.observer = ObserverHelper.narrow(this._poa().servant_to_reference(this.obsServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.observer;
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
