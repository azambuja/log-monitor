package scs.demos.logmonitor.servant;
import java.util.ArrayList;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.servant.IComponentServant;
import scs.demos.logmonitor.LogMonitor;
import scs.demos.logmonitor.LogMonitorHelper;

public class LogMonitorComponent extends IComponentServant {
	private final static String IFACE_LOGMONITOR = "scs::demos::logmonitor::LogMonitor";
	private final static String FACET_LOGMONITOR = "LogMonitor";
	private static final String FACET_INFO = "infoReceptacle";
	private static final String IFACE_INFO = "scs::core::IReceptacles";
	private LogMonitorServant logMonitorServant = null;
	private LogMonitor logMonitor = null;
	private InfoServant infoServant = null;
	private IReceptacles info = null;

	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.interface_name = IFACE_LOGMONITOR;
		fd.name = FACET_LOGMONITOR;
		fd.facet_ref = getLogMonitor();
		facets.add(fd);

		fd = new FacetDescription();
		fd.interface_name = IFACE_INFO;
		fd.name = FACET_INFO;
		fd.facet_ref = this.getInfo();
		facets.add(fd);

		return facets;
	}

	private LogMonitor getLogMonitor() {
		if (this.logMonitorServant == null) {
			try {
				this.logMonitorServant = new LogMonitorServant(this);
				this.logMonitor = LogMonitorHelper.narrow(this._poa()
					.servant_to_reference(this.logMonitorServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.logMonitor;
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
