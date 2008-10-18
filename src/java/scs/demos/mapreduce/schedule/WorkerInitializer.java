package scs.demos.mapreduce.schedule;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.omg.PortableServer.Servant;

import scs.container.ComponentAlreadyLoaded;
import scs.container.ComponentCollection;
import scs.container.ComponentCollectionHelper;
import scs.container.ComponentHandle;
import scs.core.ComponentId;
import scs.container.ComponentLoader;
import scs.container.ComponentLoaderHelper;
import scs.container.ComponentNotFound;
import scs.container.LoadFailure;
import scs.core.AlreadyConnected;
import scs.core.ExceededConnectionLimit;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidConnection;
import scs.core.InvalidName;
import scs.core.StartupFailed;
import scs.core.ConnectionDescription;
import scs.event_service.ChannelFactory;
import scs.event_service.ChannelFactoryHelper;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;
import scs.event_service.NameAlreadyInUse;
import scs.event_service.servant.ConnectionStatus;
import scs.event_service.servant.EventSinkConsumerServant;
import scs.execution_node.ContainerAlreadyExists;
import scs.execution_node.ExecutionNode;
import scs.execution_node.ExecutionNodeHelper;
import scs.execution_node.Property;
import scs.demos.mapreduce.Worker;
import scs.demos.mapreduce.WorkerHelper;
import scs.demos.mapreduce.Task;
import scs.demos.mapreduce.TaskHelper;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.IOFormat;
import scs.demos.mapreduce.IOFormatHelper;
import scs.demos.mapreduce.IOFormatException;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.SplitException;
import scs.demos.mapreduce.FileSplitHelper;
import scs.demos.mapreduce.servant.MapTaskServant;
import scs.execution_node.InvalidProperty;

import java.util.Hashtable;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Objeto para inicializar os execution nodes, criando os containers e os
 * componentes workers
 * @author Sand Luz Correa
*/

public class WorkerInitializer {

        private String[] execNodeList;
        private MasterServant master;
	private ORB orb;
	
        private static final String EXEC_NODE_NAME = "ExecutionNode";
	private static final String EXEC_NODE_FACET = "scs::execution_node::ExecutionNode";
        private static final String SUFIX = ".txt";

	private static final String MAP_REDUCE_CONTAINER = "MapReduceContainer";

	private  Hashtable hashNodes ;
	private DataInputStream in = null;
	private POA poa = null;  
        private int[] mappers_p_nodes;
	private Reporter reporter = null;
        private String configFileName;
        private IOFormat ioformat = null;  
	private String exception;
	private int mapped;
 
	public WorkerInitializer(MasterServant master) throws Exception {
		try {
			this.master = master;
     			this.orb = master.getOrb();
     			this.execNodeList = master.getExecNodeList();
     			this.reporter = master.getReporter();
            		this.configFileName = master.getConfigFileName();
            		this.mapped = 0;
           
     			hashNodes = new Hashtable ();
	     
			poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
					poa.the_POAManager().activate();
		} catch (Exception e) {
			exception = LogError.getStackTrace(e);
			reporter.report(0, "WorkerInitializer::WorkerInitializer - Erro ao criar WorkerInializer. \n" + exception);
                        throw e;
	    	}
	}

    	/* Retorna um execution node dado o nome do host*/
	private ExecutionNode getNode(String host,String port) {
		try {     
			String corbaname = "corbaname::" + host + ":" + port + "#"
				            + EXEC_NODE_NAME;

	     		ExecutionNode execNode = null;
	     		reporter.report(1,"WorkerInitializer::getNode - Conectando ao execution node " + corbaname);

	              	org.omg.CORBA.Object obj = orb.string_to_object(corbaname);
			IComponent execNodeComp = IComponentHelper.narrow(obj);
			execNodeComp.startup();
			Object ob = execNodeComp.getFacet(EXEC_NODE_FACET);
			execNode = ExecutionNodeHelper.narrow(ob);
                    	return execNode;

	 	} catch (SystemException ex) {
                        exception = LogError.getStackTrace(ex);
			reporter.report(0,"WorkerInitializer::getNode - Erro ao conectar com o ExecutionNode " + host + 
					  ".\n" + exception);
                        return null; 
	      	} catch (StartupFailed e) {
                        exception = LogError.getStackTrace(e);
			reporter.report(0,"WorkerInitializer::getNode - Startup do ExecutionNode " + host + " falhou. \n"
					   + exception);
			return null;
	      	}
   	}

    	/* Cria um container no execution node especificado */
	private boolean createContainer(String name, ExecutionNode execNode) {
		try {
			Property prop = new Property();
			prop.name = "language";
			prop.value = "java";
			Property propSeq[] = { prop };
			IComponent container = execNode.startContainer(name, propSeq);
			if (container == null) {
				return false;
			}
                        return true;
		
		} catch (ContainerAlreadyExists e) {
			reporter.report(0,"WorkerInitializer::createContainer - Ja existe um container com este nome:" + name);
			return false;
	   } catch (InvalidProperty e) {
	   	    reporter.report(0, "WorkerInitializer::createContainer - Erro ao setar propriedades do container");
	        return false;
	   }	    
	   
	}

	/* Cria um component no container associado a loader*/
	private ComponentHandle createHandle(ComponentLoader loader, ComponentId compId){
                ComponentHandle handle = null;

		try {
	      		handle = loader.load(compId, new String[] { "" });
	      		handle.cmp.startup();
		} catch (ComponentNotFound e) {
	      		reporter.report(0,"WorkerInitializer::createHandle - Componente " + compId.name + " nao encontrado.");
		} catch (ComponentAlreadyLoaded e) {
			reporter.report(0,"WorkerInitializer::createHandle - Componente " + compId.name + " já foi criado.");
		} catch (LoadFailure e) {
                        exception = LogError.getStackTrace(e);
			reporter.report(0,"WorkerInitializer::createHandle - Erro ao carregar componente " + compId.name + ".\n"
					   + exception);
		} catch (StartupFailed e) {
                        exception = LogError.getStackTrace(e);
                        reporter.report(0,"WorkerInitializer::createHandle - Startup do componente " + compId.name + " falhou.\n" 
					   + exception);
            	}

		return handle;
	}
	
	/* Instancia um servant IOFormatServant dado o nome da classe */
	public IOFormat createIOFormatServant(String servantName) {
		IOFormat ioformat = null;

	     	try{	
	       		Servant obj = (Servant) Class.forName(servantName).newInstance();
	       		ioformat = IOFormatHelper.narrow(this.poa.servant_to_reference(obj));
	     	} catch (Exception e){
	     		exception = LogError.getStackTrace(e);
	     	    	reporter.report(0,"WorkerInitializer::createIOFormatServant - Erro ao criar servant ioformat " +  servantName +
					   ".\n" + exception);
           	} 
	     	return ioformat;
	}
       
	/* Conecta aos execution nodes onde os workers serao criados */  	
    	public Hashtable connectToExecNodes(){
                String port = master.getMasterPort();
		for(int i = 0; i < execNodeList.length; i++){
	       		ExecutionNode execNode = getNode(execNodeList[i],port);
	        
	       		if (execNode == null)
				return null;
	       		else	
	       			hashNodes.put(execNodeList[i], execNode);
	    	}
	    
		return hashNodes;  
	}

    	/* Cria um canal de evento para comunicacao entre master e workers */
	public IComponent buildChannel(){
		ExecutionNode execNode = getNode(master.getMasterHost(),master.getMasterPort());
        	if (execNode == null)
	        	return null;	    
     	    
	    	IComponent container = execNode.getContainer(master.getContainerName());
            	if (container == null){
			reporter.report (0,"WorkerInitializer::builChannel - Erro ao retornar container do componente Master");
		       	return null;
	    	} 

	    	ComponentLoader loader = ComponentLoaderHelper.narrow(container
				     .getFacet("scs::container::ComponentLoader"));
	    	if (loader == null) {
	    		reporter.report(0,"WorkerInitializer::buildChannel - Erro ao retornar loader do container master");
	    		return null;
	    	}
		
	    	ComponentHandle handle = null;
	    	ComponentId newComponent = new ComponentId();
	    	newComponent.name = "EventManager";
	    	newComponent.version = 1;

	    	handle = createHandle(loader, newComponent);
	   	if (handle == null) {
			return null;
		}
	    
	    	IComponent eventMgr = handle.cmp;
	
	    	ChannelFactory chFactory = ChannelFactoryHelper.narrow(eventMgr.getFacet("scs::event_service::ChannelFactory"));
	    	if( chFactory== null ) {
	        	reporter.report(0,"WorkerInitializer::buildChannel - Erro ao retornar ChannelFactory !");
	          	return null;
	    	}

	    	/* Cria o eventSink para se conectar ao canal de evento*/
	    	ConnectionStatus connStatus = null; 
            	EventSinkConsumerServant servant = null; 
	    	org.omg.CORBA.Object servantObj = null;
            	IComponent masterChannel = null;

            	try {
                        connStatus = new ConnectionStatus(false);
			servant = new EventSinkMaster(connStatus,master); 
	        	servantObj = this.poa.servant_to_reference(servant);
	    
	    		EventSink eventSink = EventSinkHelper.narrow(servantObj);
            		if (eventSink == null){
				reporter.report(0,"WorkerInitializer::buildChannel - Erro ao retornar eventSink");
                		return null;
	    		}
	    
			masterChannel = chFactory.create("MasterChannel");
			masterChannel.startup();
			IReceptacles evSource = IReceptaclesHelper.narrow(masterChannel.getFacet("scs::core::IReceptacles")); 
		
			/* Registra o eventsink no channel selecionado */
		      	int connID=0;
		      	connID = evSource.connect("EventSource", eventSink);
			connStatus.setConnected(true);

                        return masterChannel; 
	     	} catch (Exception e) {
                        exception = LogError.getStackTrace(e);
		        reporter.report(0,"WorkerInitializer::buildChannel - Erro ao instanciar channel.\n" +  exception);
                        return null;
	        }	
	}

    	/* Constroi a lista de Workers */
    	public LinkedBlockingQueue buildWorkerQueue() {
		String containerName; 	
	     	LinkedBlockingQueue<Worker> workerQueue = new LinkedBlockingQueue<Worker>();

	     	ComponentId workerCompId = new ComponentId();
	     	workerCompId.name = "Worker";
	     	workerCompId.version = 1;	

	     	ComponentHandle handle = null;
            	reporter.report(1, "WorkerInitializer::buildWorkerQueue - Comecando balanceamento de carga");

            	/*
	      	 * Balanceamento de carga entre os nos.
	      	 * Calcula o numero de mapper por no
	         */
	     	int remainder = master.getNum_Mappers() % execNodeList.length;
	     	mappers_p_nodes = new int[execNodeList.length];
           	for (int i = 0; i < execNodeList.length; i++) {
			if (i < remainder)  
                		mappers_p_nodes[i] = master.getNum_Mappers() / execNodeList.length + 1;
	            	else 
                      		mappers_p_nodes[i] = master.getNum_Mappers() / execNodeList.length;
	           
		   	reporter.report(1,"WorkerInitializer::buildWorkerQueue - mappers_p_nodes[i]= " + mappers_p_nodes[i]);
	     	}

            	/* Criacao dos workers */
            	for(int i = 0; i < execNodeList.length; i++){
			ExecutionNode execNode = (ExecutionNode) hashNodes.get(execNodeList[i]);
                   
                   	for (int j = 0; j < mappers_p_nodes[i] ; j++){
                        	containerName = MAP_REDUCE_CONTAINER + j;
                     
	        		if (!this.createContainer(containerName, execNode)) {
					reporter.report(0,"WorkerInitializer::buildWorkerQueue - Erro criando o container " 
                                                           + containerName);
			          	return null;
		           	}
		
		           	IComponent container;
		           	container = execNode.getContainer(containerName);

		           	try {
		                	container.startup();
		           	} catch (StartupFailed e) {
                                        exception = LogError.getStackTrace(e);
		                	reporter.report(0,"WorkerInitializer::buildWorkerQueue - Erro no startup do container " 
                                                          + containerName + ".\n" + exception);
		                   	return null; 
		           	}

		           	ComponentLoader loader = ComponentLoaderHelper.narrow(container
                                                         .getFacet("scs::container::ComponentLoader"));
		           	if (loader == null) {
			        	reporter.report(0,"WorkerInitializer::buildWorkerQueue - Erro retornando componentLoader" +
                                                          " de workers");
			                return null;
		           	}

         		   	handle = createHandle(loader, workerCompId);
			   	if (handle == null)
					return null;

			   	WorkerHelper.narrow(handle.cmp.getFacetByName("Worker"));
			   	workerQueue.add(WorkerHelper.narrow(handle.cmp.getFacetByName("Worker")));
		   	}
	     	} 
	     	return workerQueue;
	}

    	/* Constroi uma lista de tarefas map */
	public LinkedBlockingQueue buildTaskQueue () {
		try {
			LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue<Task>();
                	
                	ioformat = master.getIOFormat();
                	FileSplit[] splits = ioformat.getSplits(configFileName, reporter);
                 
                	for(int i=0; i<splits.length; i++) {
				Task t = TaskHelper.narrow(this.poa.servant_to_reference(
					 new MapTaskServant(configFileName, reporter, poa, splits[i])));
                         	taskQueue.add(t);
			}
               		return taskQueue;
		} catch (Exception e) {
                       	exception = LogError.getStackTrace(e);
			reporter.report(0,"WorkerInitializer::buildTask - Erro ao retornar particoes do arquivo.\n" + exception);
			return null;
		}
	}

    	/* destroi os containers mapreduce criados */
    	public void finish() {
                try{
               		String containerName;
        		for(int i = 0; i < execNodeList.length; i++){
				ExecutionNode execNode = (ExecutionNode) hashNodes.get(execNodeList[i]);
                        	reporter.report(1,"WorkerInitializer::finish - Finalizando containers MAPREDUCE");
              			for (int j = 0; j < mappers_p_nodes[i] ; j++){
                             		containerName = MAP_REDUCE_CONTAINER + j;
                          		execNode.stopContainer(containerName);
                        	}
               		}
		} catch (Exception e){
                       exception = LogError.getStackTrace(e);
                       reporter.report(0,"WorkerInitializer::finish - Erro ao finalizar containers.\n" + exception);
                       return;
                }
    	}
}
             

	

