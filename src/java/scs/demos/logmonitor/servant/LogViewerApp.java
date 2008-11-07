package scs.demos.logmonitor.servant;

import gnu.getopt.Getopt;

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
import scs.demos.logmonitor.LogViewer;
import scs.demos.logmonitor.LogViewerHelper;
import scs.demos.logmonitor.servant.EventSinkViewerServant;
import scs.execution_node.ContainerAlreadyExists;
import scs.execution_node.ExecutionNode;
import scs.execution_node.ExecutionNodeHelper;
import scs.execution_node.InvalidProperty;
import scs.execution_node.Property;

import java.io.*;

public class LogViewerApp {

	private static final String EXEC_NODE_NAME = "ExecutionNode";
	private static final String EXEC_NODE_FACET = "scs::execution_node::ExecutionNode";
	private static final String LV_CONTAINER_NAME = "LogViewerContainer";
	private static final String EV_CONTAINER_NAME = "EventChannelContainer";
	private IComponent logMonitorComp = null;
	private IComponent logViewerComp = null;
	private ExecutionNode[] execNode = null;
	private String exception;

	public LogViewerApp(String ecHostname, String ecPort, String lvHostname, String lvPort) {
		if (!initialize(ecHostname, ecPort, lvHostname, lvPort)) {
			System.err.println("Erro iniciando a aplicacao");
			System.exit(1);
		}
	}


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

	private boolean initialize(String ecHostname, String ecPort, String lvHostname, String lvPort) {
		ORB orb = null;
		String[] ecArgs = new String[2];
		ecArgs[0] = ecHostname;
		ecArgs[1] = ecPort;
		
		String[] viewerArgs = new String[2];
		viewerArgs[0] = lvHostname;
		viewerArgs[1] = lvPort;

		execNode = new ExecutionNode[2];
		String ecCorbaname = "corbaname::" + ecHostname + ":" + ecPort + "#" + EXEC_NODE_NAME ;
		String lvCorbaname = "corbaname::" + lvHostname + ":" + lvPort + "#" + EXEC_NODE_NAME ;

		System.out.println("Conectando ao execution node: " + ecCorbaname);

		//Connecting to LogMonitor/EventManager/EventChannel Execution Node
		try {
			orb = ORB.init(ecArgs, null);
			org.omg.CORBA.Object obj = orb.string_to_object(ecCorbaname);
			IComponent execNodeComp = IComponentHelper.narrow(obj);
			Object ob = execNodeComp.getFacet(EXEC_NODE_FACET);
			execNode[0] = ExecutionNodeHelper.narrow(ob);
		} catch (SystemException ex) {
			System.err.println("Erro ao conectar com o ExecutionNode " + ecCorbaname);
			System.exit(1);
		}

		//Creating Log Viewer Execution Node
		try {
			orb = ORB.init(viewerArgs, null);
			org.omg.CORBA.Object obj = orb.string_to_object(lvCorbaname);
			IComponent execNodeComp = IComponentHelper.narrow(obj);
			execNodeComp.startup();
			Object ob = execNodeComp.getFacet(EXEC_NODE_FACET);
			execNode[1] = ExecutionNodeHelper.narrow(ob);
		} catch (SystemException ex) {
			System.err.println("Erro ao conectar com o ExecutionNode " + lvCorbaname);
			System.exit(1);
		} catch (StartupFailed e) {
			System.err.println("Startup do ExecutionNode " + lvCorbaname + "falhou.");
			System.exit(1);
		}

		//Creating Log Viewer Container
		if (!this.createContainer(LV_CONTAINER_NAME, execNode[1])) {
			System.err.println("Erro criando o container em " + lvCorbaname);
			return false;
		}

		//Retrieving LogMonitor/EventChannel Container
		IComponent evContainer;
		evContainer = execNode[0].getContainer(EV_CONTAINER_NAME);

		//Retrieving LogViewer Container
		IComponent viewerContainer;
		viewerContainer = execNode[1].getContainer(LV_CONTAINER_NAME);

		//Starting Container
		try {
			viewerContainer.startup();
		} catch (StartupFailed e) {
			System.out.println("Erro no startup do container em " + lvCorbaname);
			System.exit(1);
		}

		//Getting Component Collection Facet from container
		ComponentCollection compCollection = ComponentCollectionHelper.narrow(evContainer
			.getFacet("scs::container::ComponentCollection"));
		if (compCollection == null) {
			System.out.println("Erro ao retornar faceta loader em " + ecCorbaname);
			return false;
		}

		//Getting Component Loader Interface
		ComponentLoader loader = ComponentLoaderHelper.narrow(viewerContainer
			.getFacet("scs::container::ComponentLoader"));
		if (loader == null) {
			System.out.println("Erro ao retornar faceta loader em " + lvCorbaname);
			return false;
		}

		//Getting Event Manager Reference
		ComponentHandle eventMgrHandle = null;
		ComponentId eventMgrCompId = new ComponentId();
		eventMgrCompId.name = "EventManager";
		eventMgrCompId.version = 1;

		ComponentHandle [] handles = compCollection.getComponent(eventMgrCompId);
		eventMgrHandle = handles[0];
		if (eventMgrHandle == null) {
			return false;
		}

		IComponent eventMgr = eventMgrHandle.cmp;

		//Loading Log Viewer Component
		ComponentHandle logViewerHandle = null;
		ComponentId logViewerCompId = new ComponentId();
		logViewerCompId.name = "LogViewer";
		logViewerCompId.version = 1;

		logViewerHandle = createHandle(loader, logViewerCompId);
		if (logViewerHandle == null) {
			return false;
		}

		logViewerComp = logViewerHandle.cmp;

		//Getting EventSink Facet from LogViewer Component
		EventSink eventSinkViewer = EventSinkHelper.narrow(logViewerComp.getFacet("scs::demos::logmonitor::EventSink"));
		if( logViewerComp.getFacetByName("EventSink") == null ) {
			System.out.println("WorkerInitializer::buildChannel - Erro ao retornar eventSinkViewer !");
			return false;
		}

		//Getting Channel Collection Facet from Event Manager Component
		ChannelCollection chCollection = ChannelCollectionHelper.narrow(eventMgr.getFacet("scs::event_service::ChannelCollection"));
		if( chCollection == null ) {
			System.out.println("WorkerInitializer::buildChannel - Erro ao retornar ChannelCollection !");
			return false;
		}

		IComponent masterChannel = null;

		try {
			//Getting event channel
			masterChannel = chCollection.getChannel("MasterChannel");
			IReceptacles eventSource = IReceptaclesHelper.narrow(masterChannel.getFacet("scs::core::IReceptacles"));

			//Connecting log viewer with event channel
			try {
				eventSource.connect("EventSource", eventSinkViewer);
			} catch (Exception e) {
				System.out.println("WorkerInitializer::buildChannel - Erro ao conectar source no sink." + e.getMessage());
				return false;
			}

		} catch (Exception e) {
			System.out.println("WorkerInitializer::buildChannel - Erro ao instanciar channel.\n");
			return false;
		}

		return true;
	}

	public static void main(String[] args) {
		int c;
		String arg;
		String ecHostname = "localhost";
		String ecPort = "1050";
		String lvHostname = "localhost";
		String lvPort = "1050";
		String logfile = "saida.txt";

		InputStreamReader isr = new InputStreamReader ( System.in );
		BufferedReader br = new BufferedReader ( isr );

		Getopt g = new Getopt("LogViewerApp", args, "e:l:p:q:o:h");

		while ((c = g.getopt()) != -1)
		{
			switch(c)
			{
				case 'e':
					arg = g.getOptarg();		            
					ecHostname = arg != null ? arg : "localhost";
					break;
				case 'l':
					arg = g.getOptarg();		            
					lvHostname = arg != null ? arg : "localhost";
					break;
				case 'p':
					arg = g.getOptarg();		            
					ecPort = arg != null ? arg : "1050";
					break;
				case 'q':
					arg = g.getOptarg();		            
					lvPort = arg != null ? arg : "1050";
					break;
				case 'o':
					arg = g.getOptarg();		            
					logfile = arg != null ? arg : "saida.txt";
					break;
				case 'h':
				case '?':
					System.out.println("Uso: LogViewerApp [-e hostname] [-p porta] [-l hostname] [-q porta] [-o pathname] [-h]");
					System.out.println("\t-e <hostname>\t\tHostname do Execution Node para o Event Channel (default = localhost)");
					System.out.println("\t-p <hostname>\t\tPorta do Execution Node para o Event Channel (default = 1050)");
					System.out.println("\t-l <hostname>\t\tHostname do Execution Node para o LogViewer (default = localhost)");
					System.out.println("\t-q <hostname>\t\tPorta do Execution Node para o LogViewer (default = 1050)");
					System.out.println("\t-o <pathname>\t\tNome do path para arquivo de saída de log (default = saida.txt)");
					System.out.println("\t-h\t\t\tEssa tela de ajuda. :)");
					System.exit(1);
				break;

			}
		}

	 	System.out.println("Execution Node para o Event Channel: " + ecHostname + ":" + ecPort);
	 	System.out.println("Execution Node para o LogViewer: " + lvHostname + ":" + lvPort);
		System.out.println("Arquivo de saída de log: " + logfile);

		try {
			long start = System.currentTimeMillis();
			LogViewerApp app = new LogViewerApp(ecHostname, ecPort, lvHostname, lvPort);

			app.run(logfile);

			System.out.print("Press any key to stop monitoring ... ");
			br.readLine();

			long end = System.currentTimeMillis();
			
			System.out.println("Tempo total de execucao:" + (end - start));
			
			app.stop();
		} catch (SystemException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void run(String logfile) throws InterruptedException {
		//Retrieving LogViewer Facet
		LogViewer viewer = LogViewerHelper.narrow(logViewerComp.getFacet("scs::demos::logmonitor::LogViewer"));
		//Setting log file path, passed via arguments
		viewer.setLogFile(logfile);

	}

	public void stop() {
		try{
			execNode[1].stopContainer(LV_CONTAINER_NAME); 
			Thread.sleep(1000);
		} catch (Exception e ) {
			System.err.println("Erro ao finalizar container");
		}
	}
}
