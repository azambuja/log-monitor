package scs.demos.mapreduce.schedule;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;

import scs.core.IComponent;

import scs.demos.mapreduce.Master;
import scs.demos.mapreduce.MasterHelper;
import scs.demos.mapreduce.MasterPOA;
import scs.demos.mapreduce.Task;
import scs.demos.mapreduce.TaskStatus;
import scs.demos.mapreduce.PropertiesException;
import scs.demos.mapreduce.ConectionToExecNodesException;
import scs.demos.mapreduce.WorkerInstantiationException;
import scs.demos.mapreduce.TaskInstantiationException;
import scs.demos.mapreduce.ChannelException;
import scs.demos.mapreduce.StartFailureException;
import scs.demos.mapreduce.Worker;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.IOFormat;
import scs.demos.mapreduce.FileSplit;

import java.util.Hashtable;
import java.util.Properties;
import java.lang.Integer;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;

/**
 * Servant que implementa a interface scs::demos::mapreduce::Master
 * @author Sand Luz Correa
*/

enum Phase {MAP,REDUCE,ERROR};

public class MasterServant extends MasterPOA {

        /* Nome do arquivo de dados a ser processado pelas funções map e reduce*/	
        private String inputFileName;
	/* Lista de endereços dos nós de execução usados no processamento */	
        private String[] execNodeList;
	/* numero de mappers */	
        private int num_mappers;
	/* numero de reducers */	
        private int num_reducers;
        /* nome do container onde master executa*/
	private String containerName;
        /* nome do servant mapper */
	private String mapperServantName;
 	/* nome do servant reducer */
	private String reducerServantName;
 	/* nome do servant partitioner */
	private String partitionerServantName;
 	/* nome do servant combiner */
	private String combinerServantName;
        /* nome do host onde master executa*/
        private String masterHost;
        /* port onde master executa*/
        private String masterPort;
	/* nome do arquivo de configuracao */
        private String configFileName;
	/* flag que indica se a operacao combine deve ser executada */
        private boolean combineFlag;
        /* nome da classe que implementa a interface IOFormat */
        private String ioformatClassName;
        /* flag que indica se os resultados dos reduces devem
	 * ser juntados em um unico arquivo
	 */
        private boolean joinFlag;
	/* nome do arquivo resultado da juncao dos reduces*/
	private String joinFileName;

	/* Lista de objetos Execution Node */
        private Hashtable hashNodes = null;
	/* Objeto que instancia workers */
	private WorkerInitializer initializer = null;
        /* referencia para o componente Master*/
	private MasterComponent myComp = null;

        private Properties config;
	private ORB orb;
	
	private LinkedBlockingQueue workerQueue;
        private LinkedBlockingQueue taskQueue;
	private IComponent channel;
        private Hashtable workingOn;
        private int num_partitions;
        private Reporter reporter;
	private IOFormat ioformat;
        private String exception;
        private Phase currentStatus;
        
	public MasterServant(MasterComponent comp){
		String[] args = new String[1];
        	args[0] = "inicio";	     
        	orb = ORB.init(args, null);
		num_partitions = 0;
		myComp = comp;
	}

	private boolean getProperties(String configFileName) {
               	this.configFileName = configFileName;
        	this.config = new Properties();

     		try {
        		config.load(new FileInputStream(configFileName));
     		} catch (IOException e) {
                        exception = LogError.getStackTrace(e);
             		reporter.report(0,"MasterServant::getProperties - Erro ao ler arquivo de configuracao " 
                 			+ configFileName + ".\n" + exception);
		 	return false;
		}

     		/* Obtendo configuracoes para o mapreduce*/
       		masterHost = this.config.getProperty("mapred.Master.corbaloc-host");
               	if (masterHost == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao obter host onde Master executara");    
        		return false;
               	}

                masterPort = this.config.getProperty("mapred.Master.corbaloc-port");
                if(masterPort == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao obter port onde Master executara");
                        return false;
		}

     		inputFileName = this.config.getProperty("mapred.Input.name");
   		if (inputFileName == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao ler nome do arquivo de entrada");    
        		return false;
       		}	

       		containerName = this.config.getProperty("mapred.Master.container-name");
     		if (containerName == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao ler nome do container do master");    
        		return false;
       		}	

     		mapperServantName = this.config.getProperty("mapred.Mapper.servant-name");
     		if (mapperServantName == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao ler nome do servant Mapper");     
        		return false;
       		}	

		reducerServantName = this.config.getProperty("mapred.Reducer.servant-name");
     		if (reducerServantName == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao ler nome do servant Reducer");     
        		return false;
       		}	

		partitionerServantName = this.config.getProperty("mapred.Partitioner.servant-name");
     		if (mapperServantName == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao ler nome do servant Partitioner");     
        		return false;
       		}	

     		String execNodes = this.config.getProperty("mapred.Workers.exec-nodes");
	    	if( execNodes == null ){ 
			reporter.report(0,"MasterServant::getProperties - Erro ao ler execution nodes " + 
                                           "onde workers serao instanciados");     
	 		return false;
     		}	 
     		else {
			execNodeList = execNodes.split(";");
     		}	    

       		num_mappers = Integer.parseInt(this.config.getProperty("mapred.Mappers.number", 
                                               String.valueOf(execNodeList.length)));
     		num_reducers = Integer.parseInt(this.config.getProperty("mapred.Reducers.number","0"));
     		if (num_mappers < execNodeList.length){
			reporter.report(0,"MasterServant::getProperties - Numero de mappers nao pode ser menor que " +
					  "numero de execution nodes");     
	 		return false;
       		}
     		if (num_mappers < num_reducers) {
			reporter.report(0,"MasterServant::getProperties - Numero de mappers nao pode ser menor que " +
					  "numero de reducers");     
	    		return false;
 		}

        	combineFlag = Boolean.valueOf(this.config.getProperty("mapred.Combine.flag","false")).booleanValue();
                if (combineFlag) {
			combinerServantName = this.config.getProperty("mapred.Combiner.servant-name", reducerServantName);
                }		
            
		ioformatClassName = this.config.getProperty("mapred.IOFormat.class-name");
        	if (ioformatClassName == null) {
			reporter.report(0,"MasterServant::getProperties - Erro ao ler nome da classe que implementa " +
                                          "a interface IOFormat");     
	    		return false;
 		}

		joinFlag = Boolean.valueOf(this.config.getProperty("mapred.Join.flag","true")).booleanValue();
		joinFileName = inputFileName.split(".txt") + "Result" + ".txt";
		joinFileName = this.config.getProperty("mapred.Join.file-name",joinFileName);
		
        	return true;
	}

        private boolean schedule() {
        	try{
               		/* hash com os workers em processamento. Indexado pela tarefa*/		     
	   		workingOn = new Hashtable();
                       	currentStatus = Phase.MAP;
	   		while(!currentStatus.equals(Phase.ERROR)) {
                        
                       		Worker worker = (Worker) workerQueue.take();
	               		Task task = (Task) taskQueue.take();
                                TaskStatus op = task.getStatus();
                           
	                	/* termina escalonamento se encontra flag de fim*/	
				if (op.value()==TaskStatus._END) {
					break;
				}
	        		workingOn.put(task, worker);
                       		worker.execute(channel, task);
               		}

                        if (currentStatus.equals(Phase.ERROR)) { 
                       		return false;
			}

                        return true; 
     		 } catch (Exception e) {
                        exception = LogError.getStackTrace(e);
               		reporter.report(0,"MasterServant::schedule - Escalonamento interrompido. \n" + exception);
                       	return false;  
     		}
	}
 
       	public void start(String configFileName, Reporter reporter) throws PropertiesException, 
                         ConectionToExecNodesException, ChannelException, 
	                  WorkerInstantiationException, TaskInstantiationException,
	                  StartFailureException {
               	this.reporter = reporter;
	        reporter.report(1,"MasterServant::start - Lendo configuracoes de " + configFileName);
			    
     	    	/* lê arquivo de configuração */
     	    	if(!getProperties(configFileName)) {
               		throw new PropertiesException();
     	    	}     
            	reporter.report(1,"MasterServant::start - Arquivo de configuracao lido");
       		
		/* cria objeto que inicializa workers e fila de tarefas */
               	try {
       			initializer = new WorkerInitializer(this);
		} catch (Exception e) {
			throw new WorkerInstantiationException();
		}
		reporter.report(1,"MasterServant::start - WorkerInitializer instanciado");
		
	        /* conecta-se com os execution nodes dos workers*/
		reporter.report(1,"MasterServant::start - Conectando aos outros execution nodes"); 
     	    	hashNodes = this.initializer.connectToExecNodes();
     	    	if (hashNodes == null) {
			throw new ConectionToExecNodesException ();
       		}
               	reporter.report(1,"MasterServant::start - Conectado ao execution node dos workers");

                /* cria canal de evento entre master e workers*/
     		reporter.report(1,"MasterServant::start - Criando canal de evento entre master e workers");
     		channel = initializer.buildChannel();
       		if (channel == null) {
		       throw new ChannelException ();
       		}

                /*instancia workers*/
     	    	reporter.report(1,"MasterServant::start - Instanciando Workers");
     	    	workerQueue = initializer.buildWorkerQueue();
     	    	if (workerQueue == null) {
		      throw new WorkerInstantiationException ();
       		}

                /*instancia tarefas*/
     	   	reporter.report(1,"MasterServant::start - Instanciando Tarefas");
       		ioformat = initializer.createIOFormatServant(ioformatClassName);
	       	if (ioformat == null) {
		       throw new TaskInstantiationException ();
	       	}

     	   	taskQueue   = initializer.buildTaskQueue();
     	   	if ((taskQueue == null) || (taskQueue.toArray().length == 0)) {
        		throw new TaskInstantiationException ();
     	   	}	
     	   	num_partitions = taskQueue.toArray().length;

                /* inicia escalonamento das tarefas map-reduce*/
       		reporter.report(1,"MasterServant::start - Iniciando escalonamento");
     	   	if (!schedule()) {
     			initializer.finish();
		       	throw new StartFailureException();
	        } 
	       	initializer.finish();	     
	}
        
	public ORB getOrb(){
		return orb;
	}

	public String[] getExecNodeList(){
		return execNodeList;
	}

	public String getContainerName() {
		return containerName;
	}

	public int getNum_Mappers() {
		return num_mappers;
	}

	public int getNum_Reducers() {
		return num_reducers;
	}

	public String getConfigFileName(){
		return configFileName;
	}

	public Hashtable getWorkingOn(){
		return workingOn;
	}

	public void addWorkerQueue(Worker w){
		workerQueue.add(w);
	}

        public void addTaskQueue(Task t){
		taskQueue.add(t);
	}

	public int getNum_Partitions (){
		return num_partitions;
	}

	public MasterComponent getComponent(){
		return myComp;
	}
        
       	public String getMasterHost(){
               	return masterHost;
       	}

	public String getMasterPort() {
		return masterPort;
	}
          
       	public void setCurrentPhase(Phase phase) {
		currentStatus = phase;
        }	
        
        public Phase getCurrentPhase() {
		return currentStatus;
	}

	public Reporter getReporter() {
		return reporter;
	}

	public IOFormat getIOFormat() {
		return ioformat;
	}

	public boolean getCombineFlag() {
		return combineFlag;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public boolean getJoinFlag() {
		return joinFlag;
	}
	public String getJoinFileName() {
		return joinFileName;
	}	
}

