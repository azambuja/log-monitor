package scs.demos.logmonitor.servant;

import scs.demos.logmonitor.LogViewer;
import scs.demos.logmonitor.LogViewerHelper;
import scs.demos.logmonitor.LogViewerPOA;

import org.omg.CORBA.Any;

public class LogViewerServant extends LogViewerPOA {

	private int identifier;
	private LogViewerComponent logviewer = null;
	private int logViewerCount; 

	public LogViewerServant(LogViewerComponent logviewer){
		this.logviewer = logviewer;
	}

	public void setId(int identifier){
		this.identifier = identifier;
	}   

	public int getId(){
		return identifier;
	}
}

