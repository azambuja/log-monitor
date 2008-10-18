package scs.demos.mapreduce.schedule;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

import scs.demos.mapreduce.WorkerPOA;
import scs.demos.mapreduce.Task;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.TaskStatus;
import scs.core.IComponent;
import scs.core.ConnectionDescription;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;
import scs.demos.mapreduce.Reporter;


/**
 * Servant que implementa a interface scs::demos::mapreduce::Worker
 * @author Sand Luz Correa
*/

public class WorkerServant extends WorkerPOA {

	Thread myself;
	String execNodeName;

	private class WorkerThread extends Thread{
		private IComponent channel;
		private Task task;
		private ORB orb;
		private EventSink evSink;
        	private String exception;
        	private String[] operations = {"map","reduce", "join"};
		Any eventAny;
		
	    	public WorkerThread(IComponent channel, Task task) {
			this.channel = channel;
			this.task = task;
            		this.orb = ORB.init();
			this.evSink = ((EventSink)this.channel.getFacet("scs::event_service::EventSink"));
			this.eventAny = this.orb.create_any();			
	    	}
	    
	    	public void run() {
     			TaskStatus op = task.getStatus();
        		Reporter reporter = task.getReporter();
                        FileSplit[] inputSplit = task.getInput();
			FileSplit[] outputSplit = task.getOutput();
                        int id = task.getId();
        	       	String infileName  = "";
			String outfileName = "";  
     			try {      	
				/* for(int i=0; i< inputSplit.length; i++) {
                			infileName = infileName + inputSplit[i].getPath() + " ";
            			}

				for(int i=0; i< outputSplit.length; i++) {
                			outfileName = outfileName + outputSplit[i].getPath() + " ";
            			}

            			reporter.report(2,"WokerServant::run - TaskID = " + id + "\n" +
			        	    	"    Executando " + operations[op.value()] + ".\n" + 
			            		"    Arquivos de entrada: " + infileName + "\n" +
						"    Arquivos de saida: " + outfileName); */
 
            			eventAny.insert_long(id);                		
            			task.run();
						
			} catch (Exception e) {
                    		exception = LogError.getStackTrace(e);
				reporter.report(0,"WokerServant::run - TaskID= " + id +
                                        ". Erro ao executar " + operations[op.value()] + ".\n" + 
			        exception);
                	        task.setStatus(TaskStatus.ERROR);
			}
        		evSink.push(eventAny);             
	  	}	
	}	

	public void execute (IComponent channel, Task task){
	     	Thread exec = new WorkerThread(channel, task);
	     	exec.start();
	}	     

	public boolean ping() {
		return myself.isAlive();
	}

    	public String getNode() {
       		return execNodeName;
    	}

    	public void setNode(String name) {
       		execNodeName = name;
    	}
}

