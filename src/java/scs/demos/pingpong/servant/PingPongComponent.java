package scs.demos.pingpong.servant;

import java.util.ArrayList;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.servant.IComponentServant;
import scs.demos.pingpong.PingPong;
import scs.demos.pingpong.PingPongHelper;

public class PingPongComponent extends IComponentServant {

	private final static String IFACE_PINGPONG = "scs::demos::pingpong::PingPong";

	private final static String FACET_PINGPONG = "PingPong";

	private static final String FACET_INFO = "infoReceptacle";

	private static final String IFACE_INFO = "scs::core::IReceptacles";

	private PingPongServant pingPongServant = null;

	private PingPong pingPong = null;
	
	private InfoServant infoServant = null;

	private IReceptacles info = null;

	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.interface_name = IFACE_PINGPONG;
		fd.name = FACET_PINGPONG;
		fd.facet_ref = getPingPong();
		facets.add(fd);

		fd = new FacetDescription();
		fd.interface_name = IFACE_INFO;
		fd.name = FACET_INFO;
		fd.facet_ref = this.getInfo();
		facets.add(fd);

		return facets;
	}

	private PingPong getPingPong() {
		if (this.pingPongServant == null) {
			try {
				this.pingPongServant = new PingPongServant(this);
				this.pingPong = PingPongHelper.narrow(this._poa()
						.servant_to_reference(this.pingPongServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.pingPong;
	}

	public IReceptacles getInfo() {
		this.infoServant = new InfoServant();

		try {
			this.info = IReceptaclesHelper.narrow(this._poa()
					.servant_to_reference(this.infoServant));
		} catch (ServantNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		}
		return this.info;
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
