package scs.demos.mapreduce.schedule;

import java.util.ArrayList;
import java.util.Properties;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.servant.IComponentServant;
import scs.demos.mapreduce.Master;
import scs.demos.mapreduce.MasterHelper;



/**
 * Servant do IComponent que oferece a faceta scs::demos::mapreduce::Master
 * 
 * @author Sand Luz Correa
*/
public class MasterComponent extends IComponentServant {

	private MasterServant masterServant = null;
	private Master master = null;

	private MonitoringReceptacles monitoringReceptaclesServant = null;
	private IReceptacles monitoringReceptacles = null;
	
	private final static String IFACE_MASTER = "scs::demos::mapreduce::Master";
	private final static String FACET_MASTER = "Master";

        private static final String IFACE_REC = "scs::core::IReceptacles";
	private static final String FACET_REC = "Monitoring";


	
        
        /* (non-Javadoc)
	 * @see SCS.servant.IComponentServant#createFacets()
	 */
	@Override
	protected ArrayList<FacetDescription> createFacets() {
		
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.interface_name = IFACE_MASTER;
		fd.name= FACET_MASTER;
		fd.facet_ref = getMaster();
		facets.add(fd);

		fd = new FacetDescription();
		fd.interface_name = IFACE_REC;
		fd.name= FACET_REC;
		fd.facet_ref = getReceptacles();
		facets.add(fd);
		
		return facets;
	}

	
	private Master getMaster() {
		if( this.masterServant == null ) {
			try {
				this.masterServant = new MasterServant(this);
				this.master = MasterHelper.narrow(this._poa().servant_to_reference(this.masterServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.master;
	}

	private IReceptacles getReceptacles() {
		if( this.monitoringReceptaclesServant == null ) {
			try {
				this.monitoringReceptaclesServant = new MonitoringReceptacles();
				this.monitoringReceptacles = IReceptaclesHelper.narrow(this._poa().servant_to_reference
						             (this.monitoringReceptaclesServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.monitoringReceptacles;
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
