package scs.demos.mapreduce.servant;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

import scs.demos.mapreduce.TaskPOA;
import scs.demos.mapreduce.TaskStatus;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.IOMapReduceException;

/**
 * Servant generico que implementa a interface scs::demos::mapreduce::Task
 * É especializado para encapsular tarefas map e reduce 
 * @author Sand Luz Correa
*/

public abstract class TaskServant extends TaskPOA {

	private static int taskId = 0;
        protected FileSplit[] inputSplit = null;
        protected FileSplit[] outputSplit = null; 
        protected TaskStatus status = null;
        protected int id = 0;
        protected Properties conf = null;	
        protected Reporter reporter = null;
        protected String configFileName;	
        
        public TaskServant(String configFileName, Reporter reporter) throws IOException {
		try {
                        conf = new Properties(); 
        		conf.load(new FileInputStream(configFileName));
                        this.reporter = reporter;
                        this.configFileName = configFileName;
                        id = taskId++;
     		} catch (IOException e) {
                        throw e;
		}
	}

	protected abstract void doRun() throws IOMapReduceException;
       
        public void run() throws IOMapReduceException {
		doRun();
	}

        public int getId() {
		return id;
	}
	
	public void setStatus(TaskStatus status) {
		this.status = status;
	}
	
	public TaskStatus getStatus() {
		return status;
	}
        
        public FileSplit[] getInput() {
		return inputSplit;
	}

        public FileSplit[] getOutput() {
		return outputSplit;
	}
        
        public Reporter getReporter() {
		return reporter;
	}
}         

