package scs.demos.mapreduce.schedule;

import java.util.Enumeration;
import java.util.ArrayList;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.demos.mapreduce.Task;
import scs.demos.mapreduce.TaskHelper;
import scs.demos.mapreduce.Worker;
import scs.demos.mapreduce.TaskStatus;
import scs.demos.mapreduce.servant.TaskServant;
import scs.demos.mapreduce.servant.MapTaskServant;
import scs.demos.mapreduce.servant.ReduceTaskServant;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.FileSplitHelper;
import scs.demos.mapreduce.Reporter;
import scs.event_service.servant.ConnectionStatus;
import scs.event_service.servant.EventSinkConsumerServant;
import java.util.Hashtable;

public class EventSinkMaster extends EventSinkConsumerServant {

	private MasterServant master;
	private ORB orb;
	private POA poa;
    	
	private Reporter reporter;
    	private static final String SUFIX = ".txt";
    	private static final int NUNBER_OF_OPERATIONS = 6;
    	private String exception;
    	private int mapped = 0;
        private int reduced = 0;
        private int numReducers = 0;
        private int numPartitions = 0;
        private String configFileName = null; 

        private ArrayList<FileSplit>[] inputToReducers = null; 
            	  
	public EventSinkMaster(ConnectionStatus cs, MasterServant master) throws AdapterInactive, 
                                                                          org.omg.CORBA.ORBPackage.InvalidName{
		super(cs);
    		this.master = master;
		this.orb = this.master.getOrb();
       		
      	  	this.reporter = master.getReporter();
		this.numReducers = master.getNum_Reducers();
                this.configFileName = master.getConfigFileName();
                this.inputToReducers = new ArrayList [numReducers];
              
                for(int i=0;i<numReducers;i++) {
			inputToReducers[i] = new ArrayList<FileSplit>();
		}
        				
        	try {
			this.poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			this.poa.the_POAManager().activate();
        	} catch (AdapterInactive e) {
           		exception = LogError.getStackTrace(e);
			reporter.report(0,"EventSinkMaster::EventSinkMaster - " + exception);
        	} catch (org.omg.CORBA.ORBPackage.InvalidName e) {
           		exception = LogError.getStackTrace(e);
			reporter.report(0,"EventSinkMaster::EventSinkMaster - " + exception);
		}
	}
	
	/* prepara tarefa de saida *
     	 * finishingStatus = 0 : finalizacao com erro
	 * finishingStatus = 1 : finalização normal
    	 */
	private Task[] doLastTask (Phase status) {
		try{ 
        		Task t[] = null; 
                        Task newTask = null;

                        if (status.equals(Phase.ERROR)) {
				master.setCurrentPhase(status);
			}		
                        
                        newTask = TaskHelper.narrow(poa.servant_to_reference(new 
				  MapTaskServant(configFileName, reporter)));	
		
			if (newTask == null) {
				throw new Exception ();                         
            		}
            		t = new Task[1];
			t[0] = newTask;
           		return t;
		} catch (Exception e) {
		    	exception = LogError.getStackTrace(e);
			reporter.report(0,"EventSinkMaster::doLastTask - Erro ao retornar tarefa de saida \n" +
			                   exception);
            		return null;
		}		
	}
	
        /* prepara tarefa reduce */											
	private Task[] doReduce()  throws Exception {
	    	try {
                        Task[] t = new Task[numReducers];  
                	for(int i=0; i<numReducers; i++) {
                                ReduceTaskServant rs= new ReduceTaskServant(configFileName, reporter, poa, inputToReducers[i], i);
                                 
                        	t[i]= TaskHelper.narrow(poa.servant_to_reference(rs));	

                    		if (t[i] == null) {
					throw new Exception("Erro ao instanciar tarefas reduces"); 
				}        		 
			}
			return t;
		} catch (Exception e) {
                        exception = LogError.getStackTrace(e);
			reporter.report(0,"EventSinkMaster::doReduce - Erro ao retornar tarefas de reduce \n" +
			                   exception);
			throw e;
		}
	}
	
	@Override
	protected void handleEvent(Any ev) {
		
		synchronized(master){
                try{
			int taskId = ev.extract_long();
			Worker worker = null;
                        Task task = null;         
			Task[] t = null;
                        numPartitions = master.getNum_Partitions();
			Enumeration<Task> enu = (master.getWorkingOn()).keys();
		
			while (enu.hasMoreElements()) {
				task = (Task) enu.nextElement();
				int id = task.getId();

				if(id == taskId) {
					worker = (Worker) master.getWorkingOn().remove(task);
					master.addWorkerQueue(worker);	      
                                	TaskStatus op = task.getStatus();
                                
					switch (op.value()){
                                        	case TaskStatus._ERROR:
							throw new Exception ("Erro ao completar tarefa " + id); 
		                        	case TaskStatus._MAP: 
                                        		mapped++;
							reporter.report(1, "EventSinkMaster::handleEvent - TaskID = " + id + "\n"
         						+ "    Este foi o map no. " + mapped + " de " 
                                                        + numPartitions);
                                                                
                                                        if (numReducers > 0) {
                                                        	FileSplit[] mapOutput = task.getOutput(); 
								for(int i=0; i<mapOutput.length;i++) {
									inputToReducers[i].add(mapOutput[i]);
                                                                } 
                                                                
                                                                if (mapped == numPartitions) {
                                                                	master.setCurrentPhase(Phase.REDUCE);
									t = doReduce();
								}
							}  
                                                	break;

						case TaskStatus._REDUCE:
							reduced++;
						        reporter.report(1,"EventSinkMaster::handleEvent - TaskID = " + id + "\n" 
                                                        + "    Este foi o reduce no. " + reduced + 
                                                        " de "  + numReducers);

                                                	if (reduced == numReducers) {
							        t = doLastTask(Phase.REDUCE);
							}
						        break;
						    				                           
						default:
							reporter.report(1,"EventSinkMaster::handleEvent - Valor diferente de map, "
                                                                        + "partition sort, reduce e merge"); 
                                        		break;
				      	} //switch
                      			task = null;	
      				  	if (t != null) { 
						for(int index = 0; index < t.length; index++) {   
				        		master.addTaskQueue(t[index]);
		                    		} //for
				      	}//if
                                        return;								
				} //if
			} //while
		   } catch (Exception e) {
			exception = LogError.getStackTrace(e);
			reporter.report(0,"EventSinkMaster::handleEvent - Erro ao executar handleEvent.\n" + exception);
            		Task[] t = doLastTask(Phase.ERROR);
			master.addTaskQueue(t[0]);
		   }
		} //synchronized
	} //method
} //class				

