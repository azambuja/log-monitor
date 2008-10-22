package scs.demos.logmonitor.servant;

import scs.demos.logmonitor.LogMonitor;
import scs.demos.logmonitor.LogMonitorHelper;
import scs.demos.logmonitor.LogMonitorPOA;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;

import org.omg.CORBA.Any;

import scs.core.ConnectionDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidName;

public class LogMonitorServant extends LogMonitorPOA {

	private int identifier;
	private LogMonitorComponent logmonitor = null;
	private IReceptacles infoReceptacle = null;
	private ConnectionDescription conns[];
	private int logMonitorCount; 

	public  LogMonitorServant(LogMonitorComponent logmonitor){
		this.logmonitor = logmonitor;
	}

	public void setId(int identifier){
		System.out.println("Setando ID: " + identifier);
		this.identifier = identifier;
	}   

	public int getId(){
		return identifier;
	}

	public void publishLog(Any logMessage) {
		infoReceptacle = IReceptaclesHelper.narrow(logmonitor.getFacetByName("infoReceptacle"));

		try {
			conns = infoReceptacle.getConnections("LogMonitor");
		} catch (InvalidName e) {
			e.printStackTrace();
		}

		System.out.println("LogMonitor " + identifier + " publish log");

		for (int i = 0; i < conns.length; i++) {
			EventSink eventChannelFacet = EventSinkHelper.narrow( conns[i].objref );
			eventChannelFacet.push(logMessage);
		}
	}
}

