package scs.demos.philosopher.servant;

import java.util.ArrayList;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.servant.IComponentServant;
import scs.demos.philosopher.Philosopher;
import scs.demos.philosopher.PhilosopherHelper;

public class PhilosopherComponent extends IComponentServant {

	private PhilosopherServant philosopherServant = null;
	private Philosopher philosopher = null;
	
	private HandsServant leftHandServant;
	private HandsServant rightHandServant;
	private IReceptacles left;
	private IReceptacles right;
	private InfoServant infoServant;
	private IReceptacles info;
	
	private final static String IFACE_PHILOSOPHER = "scs::demos::philosopher::Philosopher";
	private final static String FACET_PHILOSOPHER = "Philosopher";
	
	private static final String FACET_LEFT 	= "left";
	private static final String IFACE_LEFT 	= "scs::core::IReceptacles";
	
	private static final String FACET_RIGHT = "right";
	private static final String IFACE_RIGHT = "scs::core::IReceptacles";
	
	private static final String FACET_INFO 	= "info";
	private static final String IFACE_INFO 	= "scs::core::IReceptacles";
	
	public IReceptacles getLeftHand()
	{
		this.leftHandServant = new HandsServant(FACET_LEFT);
		
		try {
			this.left = IReceptaclesHelper.narrow( this._poa().servant_to_reference(this.leftHandServant));
			//System.err.println("this.left== " + this.left);
		} catch (ServantNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		}
		return this.left;
	}
	
	public IReceptacles getRightHand()
	{
		this.rightHandServant = new HandsServant(FACET_RIGHT);
		
		try {
			this.right = IReceptaclesHelper.narrow( this._poa().servant_to_reference(this.rightHandServant));
			//System.err.println("this.right == " + this.right);
		} catch (ServantNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		}
		return this.right;
	}
	
	public IReceptacles getInfo()
	{
		this.infoServant = new InfoServant();
		
		try {
			this.info = IReceptaclesHelper.narrow( this._poa().servant_to_reference(this.infoServant));
		} catch (ServantNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		}
		return this.info;
	}
	
	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd;
		
		fd = new FacetDescription();
		fd.interface_name = IFACE_LEFT;
		fd.name = FACET_LEFT;
		fd.facet_ref = this.getLeftHand();
		facets.add(fd);
		
		fd = new FacetDescription();
		fd.interface_name = IFACE_RIGHT;
		fd.name = FACET_RIGHT;
		fd.facet_ref = this.getRightHand();
		facets.add(fd);
		
		fd = new FacetDescription();
		fd.interface_name = IFACE_INFO;
		fd.name = FACET_INFO;
		fd.facet_ref = this.getInfo();
		facets.add(fd);
		
		fd = new FacetDescription();
		fd.interface_name = IFACE_PHILOSOPHER;
		fd.name= FACET_PHILOSOPHER;
		fd.facet_ref = getPhilosopher();
		facets.add(fd);
		
		return facets;
		
	}

	private Philosopher getPhilosopher() {
		if( this.philosopherServant == null ) {
			try {
				this.philosopherServant = new PhilosopherServant(this); 
				this.philosopher = PhilosopherHelper.narrow(this._poa().servant_to_reference(this.philosopherServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.philosopher;
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
