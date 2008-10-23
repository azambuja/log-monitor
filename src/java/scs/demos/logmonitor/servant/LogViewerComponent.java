package scs.demos.logmonitor.servant;
import java.util.ArrayList;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.servant.IComponentServant;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;
import scs.demos.logmonitor.servant.EventSinkViewerServant;
import scs.demos.logmonitor.LogViewer;
import scs.demos.logmonitor.LogViewerHelper;

public class LogViewerComponent extends IComponentServant {
	private final static String IFACE_EVENTSINKVIEWER = "scs::demos::logmonitor::EventSink";
	private final static String FACET_EVENTSINKVIEWER = "EventSink";
	private final static String IFACE_LOGVIEWER = "scs::demos::logmonitor::LogViewer";
	private final static String FACET_LOGVIEWER = "LogViewer";
	private EventSinkViewerServant eventSinkViewerServant = null;
	private EventSink eventSinkViewer = null;
	private LogViewerServant logViewerServant = null;
	private LogViewer logViewer = null;

	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		FacetDescription fd = new FacetDescription();
		fd.interface_name = IFACE_EVENTSINKVIEWER;
		fd.name = FACET_EVENTSINKVIEWER;
		fd.facet_ref = getEventSink();
		facets.add(fd);

		fd = new FacetDescription();
		fd.interface_name = IFACE_LOGVIEWER;
		fd.name = FACET_LOGVIEWER;
		fd.facet_ref = getLogViewer();
		facets.add(fd);

		return facets;
	}
	
	private EventSink getEventSink() {
		if(this.eventSinkViewerServant == null)
		{
			this.eventSinkViewerServant = new EventSinkViewerServant(this);
			try {
				this.eventSinkViewer = EventSinkHelper.narrow( this._poa().servant_to_reference(this.eventSinkViewerServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.eventSinkViewer;
	}
	
	private LogViewer getLogViewer() {
		if (this.logViewer == null) {
			try {
				this.logViewerServant = new LogViewerServant(this);
				this.logViewer = LogViewerHelper.narrow(this._poa()
					.servant_to_reference(this.logViewerServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return this.logViewer;
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
