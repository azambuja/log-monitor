package scs.demos.logmonitor.servant;

import java.io.*;
import scs.event_service.EventSinkPOA;
import scs.demos.logmonitor.LogViewer;
import scs.demos.logmonitor.LogViewerHelper;
import org.omg.CORBA.Any;

public class EventSinkViewerServant extends EventSinkPOA {
	private int identifier;
	private LogViewerComponent logviewer = null;
	private int logViewerCount; 

	public EventSinkViewerServant(LogViewerComponent logviewer){
		this.logviewer = logviewer;
	}

	public void push(Any event) {		
		try {		
			LogViewer viewer = LogViewerHelper.narrow(logviewer.getFacet("scs::demos::logmonitor::LogViewer"));
			String line = event.extract_string();
			BufferedWriter out = new BufferedWriter(new FileWriter(viewer.getLogFile(), true));
			System.out.println(line);
			out.write(line);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void disconnect() {
		System.out.println("EventSinkViewer desconectado!");
	}
}

