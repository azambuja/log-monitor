package scs.demos.mapreduce.servant;

import java.io.*;
import java.util.*;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnyHolder;
import org.omg.CORBA.ORB;

import scs.demos.mapreduce.OutputCollectorPOA;
import scs.demos.mapreduce.schedule.LogError;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.RecordWriter;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.IOFormat;
import scs.demos.mapreduce.TaskStatus;


/**
 * Servant que implementa a interface OutputCollector usada
 * na operação map quando o numero de reducers é igual a  
 * zero.
 */
public class DirectOutputCollectorServant extends OutputCollectorPOA {
       
	private RecordWriter out = null;
        private Reporter reporter = null;
        private IOFormat ioformat = null; 
        private FileSplit fileSplit = null;
        private String confFileName = null;
               
        public DirectOutputCollectorServant (IOFormat ioformat, Reporter reporter, FileSplit fileSplit, 
                                             String configFileName,TaskStatus status) throws IOMapReduceException {
        	try {
			this.ioformat = ioformat;
                	this.reporter = reporter;
                	this.fileSplit = fileSplit;
                        this.confFileName = configFileName;
                                        
                	out = ioformat.getRecordWriter(status);
                        out.open(confFileName, fileSplit, reporter);
		} catch (Exception e) {
                        reporter.report(0,"DirectOutputCollectorServant::DirectOutputCollectorServant -" + LogError.getStackTrace(e));
			throw new IOMapReduceException ();
		}
	}
	
	public void close() throws IOMapReduceException { 
      		try {
			if (this.out != null) {
        			out.close();
			}
      		} catch (Exception e) {
 			reporter.report(0,"DirectOutputCollectorServant::close -" + LogError.getStackTrace(e));
			throw new IOMapReduceException();
		}
    	}

    	public void flush() throws IOMapReduceException {
    	}

    	public void collect(Any key, Any value) throws IOMapReduceException {
                try {
        		out.write(key, value);
		} catch (IOMapReduceException e) {
                        reporter.report(0,"DirectOutputCollectorServant::close -" + LogError.getStackTrace(e));
			throw new IOMapReduceException();
		}
    	}
}
