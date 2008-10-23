package scs.demos.logmonitor.servant;

import scs.event_service.EventSinkPOA;

import org.omg.CORBA.Any;
import java.io.*;

public class EventSinkViewerServant extends EventSinkPOA {

	private int identifier;
	private LogViewerComponent logviewer = null;
	private int logViewerCount; 

	public EventSinkViewerServant(LogViewerComponent logviewer){
		this.logviewer = logviewer;
	}

	public void push(Any event) {
		System.out.println("Foi Pushado");
		PrintStream ps = null;
		try {
			ps = new PrintStream(new FileOutputStream("/Users/rafaelpereira/Documents/PUC/SCS/log_monitor/log-monitor/src/java/foo.log"),true);
		} catch (IOException e) {

		}	

		ps.print(event.extract_string() + "\n");
		ps.close();
	}
	
	public void disconnect() {
		System.out.println("Foi Desconectado");
	}
}

