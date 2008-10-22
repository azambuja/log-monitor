package scs.demos.logmonitor.servant;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.Any;
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
import scs.event_service.ChannelCollection;
import scs.event_service.ChannelCollectionHelper;
import scs.event_service.ChannelDescr;
import scs.event_service.ChannelFactory;
import scs.event_service.ChannelFactoryHelper;
import scs.event_service.ChannelManagement;
import scs.event_service.ChannelManagementHelper;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;
import scs.event_service.NameAlreadyInUse;
import scs.event_service.servant.ConnectionStatus;
import scs.event_service.servant.EventSinkConsumerServant;
import scs.core.AlreadyConnected;
import scs.core.ExceededConnectionLimit;
import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.IMetaInterface;
import scs.core.IMetaInterfaceHelper;
import scs.core.InvalidConnection;
import scs.core.InvalidName;
import scs.core.StartupFailed;
import scs.core.FacetDescription;
import scs.demos.logmonitor.LogMonitor;
import scs.demos.logmonitor.LogMonitorHelper;
import scs.demos.logmonitor.servant.EventSinkViewerServant;
import scs.execution_node.ContainerAlreadyExists;
import scs.execution_node.ExecutionNode;
import scs.execution_node.ExecutionNodeHelper;
import scs.execution_node.InvalidProperty;
import scs.execution_node.Property;

public class LogMonitorApp {

	private static final String EXEC_NODE_NAME = "ExecutionNode";
	private static final String EXEC_NODE_FACET = "scs::execution_node::ExecutionNode";
	private static final String CONTAINER_NAME = "LogMonitorDemoContainer";

	private ExecutionNode[] execNode = null;
	private IComponent logMonitorComp = null;
	private IComponent logViewerComp = null;
	private String exception;

	public LogMonitorApp(String[] args) {
		if (!initialize(args))
			System.err.println("Erro iniciando a aplicacao");
	}

	/**
		* Cria um container no ExecutionNode corrente
		*/
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

		} catch (ContainerAlreadyExists e) {
			System.err.println("Ja existe um container com este nome.");
			return false;
		}
		catch (InvalidProperty e) {
			System.err.println("Propriedade inválida!");
			return false;
		}
		return true;
	}
	
	
	/* Cria um component no container associado a loader*/
	private ComponentHandle createHandle(ComponentLoader loader, ComponentId compId){
		ComponentHandle handle = null;

		try {
			handle = loader.load(compId, new String[] { "" });
			handle.cmp.startup();
		} catch (ComponentNotFound e) {
			System.out.println("WorkerInitializer::createHandle - Componente " + compId.name + " nao encontrado.");
		} catch (ComponentAlreadyLoaded e) {
			System.out.println("WorkerInitializer::createHandle - Componente " + compId.name + " já foi criado.");
		} catch (LoadFailure e) {
			System.out.println("WorkerInitializer::createHandle - Erro ao carregar componente " + compId.name + ".\n");
		} catch (StartupFailed e) {
			System.out.println("WorkerInitializer::createHandle - Startup do componente " + compId.name + " falhou.\n");
		}

		return handle;
	}

	/**
		* @param args
		*/
	private boolean initialize(String[] args) {
		String host= null;
		String port = null;
		String corbaname = null;
		int id = 1;
		int numComponentPerNode;

		execNode = new ExecutionNode[1];
		args = new String[2];
		args[0] = "localhost";
		args[1] = "1050";
		numComponentPerNode = 2;


		ORB orb = ORB.init(args, null);

		for (int i=0; i < execNode.length; i++) {
			host = args[2*i];
			port = args[2*i+1]; 
			corbaname = "corbaname::" + host + ":" + port + "#"	+ EXEC_NODE_NAME ;

			System.out.println("Conectando ao execution node: " + corbaname);

			try {
				org.omg.CORBA.Object obj = orb.string_to_object(corbaname);
				IComponent execNodeComp = IComponentHelper.narrow(obj);
				execNodeComp.startup();
				Object ob = execNodeComp.getFacet(EXEC_NODE_FACET);
				execNode[i] = ExecutionNodeHelper.narrow(ob);

			} catch (SystemException ex) {
				System.err.println("Erro ao conectar com o ExecutionNode " + corbaname);
				System.exit(1);
			} catch (StartupFailed e) {
				System.err.println("Startup do ExecutionNode " + corbaname + "falhou.");
				System.exit(1);
			}


			if (!this.createContainer(CONTAINER_NAME,execNode[i])) {
				System.err.println("Erro criando o container em " + corbaname);
				return false;
			}

			IComponent container;
			container = execNode[i].getContainer(CONTAINER_NAME);

			try {
				container.startup();
			} catch (StartupFailed e) {
				System.out.println("Erro no startup do container em " + corbaname);
				System.exit(1);
			}

			ComponentLoader loader = ComponentLoaderHelper.narrow(container
				.getFacet("scs::container::ComponentLoader"));
			if (loader == null) {
				System.out.println("Erro ao retornar faceta loader em " + corbaname);
				return false;
			}

			ComponentHandle logMonitorHandle = null;
			ComponentId logMonitorCompId = new ComponentId();
			logMonitorCompId.name = "LogMonitor";
			logMonitorCompId.version = 1;

			logMonitorHandle = createHandle(loader, logMonitorCompId);
			if (logMonitorHandle == null) {
				return false;
			}

			logMonitorComp = logMonitorHandle.cmp;

			IReceptacles info1 = IReceptaclesHelper.narrow(logMonitorComp.getFacetByName("infoReceptacle"));
			if( info1 == null ) {
				System.out.println("WorkerInitializer::buildChannel - Erro ao retornar receptaculo do LogMonitor !");
				return false;
			}

			LogMonitor logMonitor = LogMonitorHelper.narrow(logMonitorComp.getFacet("scs::demos::logmonitor::LogMonitor"));
			if( logMonitor == null ) {
				System.out.println("WorkerInitializer::buildChannel - Erro ao retornar LogMonitor !");
				return false;
			}
			logMonitor.setId(1);
			System.out.println("Id do logMonitor: " + logMonitor.getId());


			ComponentHandle eventMgrHandle = null;
			ComponentId eventMgrCompId = new ComponentId();
			eventMgrCompId.name = "EventManager";
			eventMgrCompId.version = 1;

			eventMgrHandle = createHandle(loader, eventMgrCompId);
			if (eventMgrHandle == null) {
				return false;
			}

			IComponent eventMgr = eventMgrHandle.cmp;
		
			ComponentHandle logViewerHandle = null;
			ComponentId logViewerCompId = new ComponentId();
			logViewerCompId.name = "LogViewer";
			logViewerCompId.version = 1;

			logViewerHandle = createHandle(loader, logViewerCompId);
			if (logViewerHandle == null) {
				return false;
			}

			logViewerComp = logViewerHandle.cmp;

			EventSink eventSinkViewer = EventSinkHelper.narrow(logViewerComp.getFacet("scs::demos::logmonitor::EventSink"));
			if( logViewerComp.getFacetByName("EventSink") == null ) {
				System.out.println("WorkerInitializer::buildChannel - Erro ao retornar eventSinkViewer !");
				return false;
			}
			
			ChannelFactory chFactory = ChannelFactoryHelper.narrow(eventMgr.getFacet("scs::event_service::ChannelFactory"));
			if( chFactory== null ) {
				System.out.println("WorkerInitializer::buildChannel - Erro ao retornar ChannelFactory !");
				return false;
			}
			
			ChannelCollection chCollection = ChannelCollectionHelper.narrow(eventMgr.getFacet("scs::event_service::ChannelCollection"));
			if( chCollection == null ) {
				System.out.println("WorkerInitializer::buildChannel - Erro ao retornar ChannelCollection !");
				return false;
			}

			IComponent masterChannel = null;

			try {
				masterChannel = chFactory.create("MasterChannel");
				masterChannel.startup();
				EventSink eventSink = EventSinkHelper.narrow(masterChannel.getFacetByName("EventSink"));
				IReceptacles eventSource = IReceptaclesHelper.narrow(masterChannel.getFacet("scs::core::IReceptacles"));
				/*IMetaInterface aaa = IMetaInterfaceHelper.narrow(logViewerComp.getFacet("scs::core::IMetaInterface"));
				 
				FacetDescription[] facets = aaa.getFacets();

				for (int k = 0; k < facets.length; k++) {
					FacetDescription description = facets[k];
					System.out.println("Faceta: " + description.name);
				}*/
				
				
				try {
					info1.connect("LogMonitor", eventSink);
				} catch (InvalidName e) {
					e.printStackTrace();
				} catch (InvalidConnection e) {
					e.printStackTrace();
				} catch (AlreadyConnected e) {
					e.printStackTrace();
				} catch (ExceededConnectionLimit e) {
					e.printStackTrace();
				}
				
				try {
					eventSource.connect("EventSource", eventSinkViewer);
				} catch (Exception e) {
					System.out.println("WorkerInitializer::buildChannel - Erro ao conectar source no sink." + e.getMessage());
					return false;
				}
				
				ChannelDescr channels[] = chCollection.getAll();

				for (int j = 0; j < channels.length; j++) {
					ChannelDescr ch = channels[j];
					System.out.println("Canal: " + ch.name + " criado");

				}
				
				
			} catch (Exception e) {
				System.out.println("WorkerInitializer::buildChannel - Erro ao instanciar channel.\n");
				return false;
			}
		
		}
		
		
		
		
		
		return true;
	}

	public static void main(String[] args) {
		try {
			long start = System.currentTimeMillis();
			LogMonitorApp app = new LogMonitorApp(args);
			app.run();
			long end = System.currentTimeMillis();
			System.out.println("Tempo total de execucao:" + (end - start));
			app.stop();
		} catch (SystemException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void run() throws InterruptedException {
		LogMonitor log = LogMonitorHelper.narrow(logMonitorComp.getFacetByName("LogMonitor"));
		Any eventAny = ORB.init().create_any();
		eventAny.insert_string("Teste");
		log.publishLog(eventAny);

	}

	public void stop() {
		try{
			for(int i=0; i<execNode.length; i++) {
				execNode[i].stopContainer(CONTAINER_NAME); 
			}
			Thread.sleep(1000);
		} catch (Exception e ) {
			System.err.println("Erro ao finalizar container");
		}
	}
}
