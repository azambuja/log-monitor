package scs.demos.mapreduce.schedule;

import java.io.*;

import scs.demos.mapreduce.ReporterPOA;

/**
 * Servant que implementa a interface scs::demos::mapreduce::Reporter
 * Gera um log com mensagens sobre o processamento
 * @author Sand Luz Correa
*/

public class ReporterServant extends ReporterPOA {

	private String logName;
	private int executionLevel;
        private PrintStream ps = null;
		
	public ReporterServant (String logName, int level ){
           //System.out.println("ReportServant::ReportSerant: " + logName + " " + level);
	       this.logName = logName;	
	       this.executionLevel = level;		
	}

	public boolean open() {
		try {
			ps = new PrintStream(new FileOutputStream(logName),true);
			return true;
		} catch (IOException e) {
			return false;
		}	
	}

	public void close() {
        	ps.close();
	} 

	public synchronized void report(int level, String message) {
		if (level <= executionLevel) {
			ps.print(message + "\n");
		}
	}
}
	

