package scs.demos.mapreduce.app;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

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
import scs.execution_node.ContainerAlreadyExists;
import scs.execution_node.ExecutionNode;
import scs.execution_node.ExecutionNodeHelper;
import scs.execution_node.Property;

import scs.demos.mapreduce.Master;
import scs.demos.mapreduce.MasterHelper;
import scs.demos.mapreduce.PropertiesException;
import scs.demos.mapreduce.ConectionToExecNodesException;
import scs.demos.mapreduce.ChannelException;
import scs.demos.mapreduce.StartFailureException;
import scs.demos.mapreduce.WorkerInstantiationException;
import scs.demos.mapreduce.TaskInstantiationException;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.ReporterHelper;
import scs.demos.mapreduce.schedule.ReporterServant;
import scs.execution_node.InvalidProperty;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MapReduceApp {

	private Properties config;
	private static final String EXEC_NODE_NAME = "ExecutionNode";
	private static final String EXEC_NODE_FACET = "scs::execution_node::ExecutionNode";
	private ExecutionNode execNode = null;
        
	static ORB orb;
	private static Master master = null;
        private static Reporter reporter = null;
	private String containerName = null;
        private String logName = null;
        private int logLevel;
	         
	private boolean readConfiguration( String filename ) {
		Properties properties = new Properties();

		try {
			properties.load(new FileInputStream(filename));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		config = properties;
		return true;
	}
        
        private String getContainerName(){
                return containerName;
        }

        private ExecutionNode getExecutionNode(){
                return execNode;
        }

	private boolean initialize(String[] args) {
                if (args.length < 1){
			System.out.println("MapReduceApp::initialize - Informe o arquivo de configuracao da aplicacao.");
			return false;
		}

		if(!readConfiguration(args[0]))
		{
			System.out.println("MapReduceApp::initialize - Arquivo " + args[0] + " nao encontrado.");
			return false;
		}

                containerName = config.getProperty("mapred.Master.container-name", "MasterContainer");
                
		String host = config.getProperty("mapred.Master.corbaloc-host");
                if (host == null) {
			System.out.println("MapReduceApp::initialize - Host onde master executara nao foi informado");
                        return false;
		}
		
                String port = config.getProperty("mapred.Master.corbaloc-port");
                if (port == null) {
			System.out.println("MapReduceApp::Initialize - Port onde master executara nao foi informado");
                        return false;
		}
	
                logName = config.getProperty("mapred.Reporter.file-name", "report.debug");
                logLevel = Integer.parseInt(config.getProperty("mapred.Reporter.level", "0"));
 
		String corbaname = "corbaname::" + host + ":" + port + "#"
		+ EXEC_NODE_NAME;
		
		orb = ORB.init(args, null);
		
		System.out.println("MapReduceApp::initialize - Conectando ao execution node master: " + corbaname);

		try {
			org.omg.CORBA.Object obj = orb.string_to_object(corbaname);
			IComponent execNodeComp = IComponentHelper.narrow(obj);
			execNodeComp.startup();
			Object ob = execNodeComp.getFacet(EXEC_NODE_FACET);
			execNode = ExecutionNodeHelper.narrow(ob);
		} catch (SystemException ex) {
			System.out.println("MapReduceApp::initialize - Erro ao conectar com o ExecutionNode master CORBANAME:" + corbaname);
			return false;
		} catch (StartupFailed e) {
			System.out.println("MapReduceApp::initialize - Startup do ExecutionNode master falhou.");
			return false;
		}
		
		System.out.println("MapReduceApp::initialize - Criando container master " + containerName);

	        if (!this.createContainer()) {
			System.out.println("MapReduceApp::initialize - Erro criando o container master " + containerName);
			return false;
		}

		IComponent container;
		container = execNode.getContainer(containerName);

		try {
			container.startup();
		} catch (StartupFailed e1) {
			System.out.println("MapReduceApp::initialize - Erro no startup do container master " + containerName);
			return false;
		}

		ComponentLoader loader = ComponentLoaderHelper.narrow(container
				.getFacet("scs::container::ComponentLoader"));
		if (loader == null) {
			System.out.println("MapReduceApp::initialize - Erro ao obter faceta loarder do container master " + containerName);
			return false;
		}

		Object componentCollection = container
				.getFacet("scs::container::ComponentCollection");
		ComponentCollection components = ComponentCollectionHelper
				.narrow(componentCollection);

		ComponentId compId = new ComponentId();
		compId.name = "Master";
		compId.version = 1;

		System.out.println("MapReduceApp::initialize - Criando master" );
		
		ComponentHandle handle = null;

		try {
			handle = loader.load(compId, new String[] { "" });
                        handle.cmp.startup();
		} catch (ComponentNotFound e) {
			System.out.println("MapReduceApp::initialize - Componente Master nao encontrado.");
			e.printStackTrace();
			return false;
		} catch (ComponentAlreadyLoaded e) {
			ComponentHandle handles[] = components.getComponent(compId);
			handle = handles[0];
		} catch (LoadFailure e) {
			e.printStackTrace();
			return false;
		} catch (StartupFailed e) {
			e.printStackTrace();
		        return false;
	        }

		master = MasterHelper.narrow(handle.cmp
					.getFacetByName("Master"));
        
                System.out.println("MapReduceApp::initialize - Criando logger da aplicacao");

        	try {
			POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			poa.the_POAManager().activate();
			reporter = ReporterHelper.narrow(poa.servant_to_reference(new ReporterServant(logName,logLevel)));
		} catch (Exception e) {
                        System.out.println("Erro ao criar logger da aplicacao");
			e.printStackTrace();
			return false;
		} 

		return true;
	}

	/**
	 * Cria um container no ExecutionNode corrente
	 */
	private boolean createContainer() {
		try {
			Property prop = new Property();
			prop.name = "language";
			prop.value = "java";
			Property propSeq[] = { prop };
			IComponent container = execNode.startContainer(containerName, propSeq);

			if (container == null) {
				return false;
			}

		} catch (ContainerAlreadyExists e) {
			System.out.println("MapReduceApp::createContainer - Ja existe um container com este nome " + containerName);
            return false;
		} catch (InvalidProperty e) {
			System.out.println("MapReduceApp::createContainer - Erro ao setar propriedades de container " + containerName);
		    return false;
		}

		return true;
	}

        public static void main(String[] args) {
		MapReduceApp app = null;
        	try {
			long startTime = System.currentTimeMillis();
			app = new MapReduceApp();
			
			if (!app.initialize(args)){
	        		System.exit(1);
			}
    
            		reporter.open();

			System.out.println("MapReduceApp::main - Executando master");
			master.start(args[0], reporter);

            		long totalTime = (System.currentTimeMillis() - startTime)/1000;

            		System.out.println("MapReduceApp::main - MapReduce foi executado com sucesso.");
            		System.out.println("MapReduceApp::main - Tempo Total de Execucao em s: " + totalTime);
     
              		System.out.println("MapReduceApp::main - Finalizando container Master " + app.getContainerName());
                	ExecutionNode execNode = app.getExecutionNode();
                	execNode.stopContainer(app.getContainerName());
                	
		} catch (PropertiesException ex) {
			System.out.println("MapReduceApp::main - Erro ao ler arquivo de configuracao.");
                        System.out.println("MapReduceApp::main - Para maiores informacoes consulte arquivo de log");
                        System.exit(1);			
		} catch (ConectionToExecNodesException ex) {
			System.out.println("MapReduceApp::main - Erro ao conectar com os execution nodes workers.");
                        System.out.println("MapReduceApp::main - Para maiores informacoes consulte arquivo de log"); 
			System.exit(1);
		} catch (ChannelException ex) {
			System.out.println("MapReduceApp::main - Erro ao criar canal de evento.");
                        System.out.println("MapReduceApp::main - Para maiores informacoes consulte arquivo de log");
			System.exit(1);
		} catch (WorkerInstantiationException ex) {
			System.out.println("MapReduceApp::main - Erro ao instanciar workers.");
                        System.out.println("MapReduceApp::main - Para maiores informacoes consulte arquivo de log");
			System.exit(1);
		} catch (TaskInstantiationException ex) {
			System.out.println("MapReduceApp::main - Erro ao instanciar tarefas.");
                        System.out.println("MapReduceApp::main - Para maiores informacoes consulte arquivo de log");
			System.exit(1);
		} catch (StartFailureException ex) {
			System.out.println("MapReduceApp::main - Erro ao executar operacoes map-reduce.");
                        System.out.println("MapReduceApp::main - Para maiores informacoes consulte arquivo de log");
			System.exit(1);
		} catch (InvalidName e) {
			System.out.println("MapReduceApp::main - Erro ao parar container.");
            System.out.println("MapReduceApp::main - Para maiores informacoes consulte arquivo de log");
			System.exit(1);
		}
                
	}
}
