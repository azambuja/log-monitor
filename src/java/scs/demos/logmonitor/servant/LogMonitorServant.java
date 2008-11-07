package scs.demos.logmonitor.servant;

import scs.demos.logmonitor.LogMonitor;
import scs.demos.logmonitor.LogMonitorHelper;
import scs.demos.logmonitor.LogMonitorPOA;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

import scs.core.ConnectionDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidName;

import java.io.*;

public class LogMonitorServant extends LogMonitorPOA {

	private int identifier;
	private int interval;
	private boolean tailing;
	private String logfile;
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
	
	public void setMonitorInterval(int interval){
		this.interval = interval;
	}   

	public int getMonitorInterval(){
		return interval;
	}

	public void setLogFile(String logfile){
		this.logfile = logfile;
	}

	public String getLogFile(){
		return this.logfile;
	}
	
	public void setTailing(boolean tail){
		this.tailing = tail;
	}

	public void publishLog() {
		try {
			infoReceptacle = IReceptaclesHelper.narrow(logmonitor.getFacetByName("infoReceptacle"));

			// Start tailing
			this.tailing = true;
			
			BufferedInputStream bis = new
				BufferedInputStream(new FileInputStream(this.logfile));
			int length = -1;
			
			while( this.tailing ) {
				try {
					length = bis.available();
					
					if(length == -1)
						throw new IOException("file ended");
					
					if(length <= 0)
						continue;
					
					byte [] data = new byte[length];
					
					if(bis.read(data, 0, length ) == -1)
						continue;
						
					String msg = new String(data);
					
					// Deliver file text to the connected receptacles
					if(!msg.equals("")) {
						System.out.println(new String(data));
					
						Any logMessage = ORB.init().create_any();

						logMessage.insert_string(new String(data));
					
						try {
							conns = infoReceptacle.getConnections("LogMonitor");
						} catch (InvalidName e) {
							e.printStackTrace();
						}
					
						for (int i = 0; i < conns.length; i++) {
							EventSink eventChannelFacet = EventSinkHelper.narrow( conns[i].objref );
							eventChannelFacet.push(logMessage);
						}
					}
					
					Thread.sleep(interval);
				
				} catch( Exception e ) {
				}
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
	}
}

