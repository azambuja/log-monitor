package scs.demos.logmonitor.servant;

import gnu.getopt.Getopt;
import java.io.*;

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


public class EventChannelApp {

	private static final String EXEC_NODE_NAME = "ExecutionNode";
	private static final String EXEC_NODE_FACET = "scs::execution_node::ExecutionNode";
	private static final String CONTAINER_NAME = "EventChannelContainer";

	private ExecutionNode execNode = null;
	private IComponent logMonitorComp = null;
	private IComponent logViewerComp = null;
	private String exception;

	public EventChannelApp(String host, String port) {
		if (!initialize(host, port))
			System.err.println("Erro iniciando a aplicacao");
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

	/**
		* @param args
		*/
	private boolean initialize(String host, String port) {
		ORB orb = null;
		String[] args = new String[2];
		args[0] = host;
		args[1] = port;

		//First Execution Node Data - Where LogMonitor and EventChannel will run
		String corbaname = "corbaname::" + host + ":" + port + "#"	+ EXEC_NODE_NAME ;
		System.out.println("Conectando ao execution node: " + corbaname);

		//Connecting to Execution Node
		try {
			orb = ORB.init(args, null);
			org.omg.CORBA.Object obj = orb.string_to_object(corbaname);
			IComponent execNodeComp = IComponentHelper.narrow(obj);
			execNodeComp.startup();
			Object ob = execNodeComp.getFacet(EXEC_NODE_FACET);
			execNode = ExecutionNodeHelper.narrow(ob);

		} catch (SystemException ex) {
			System.err.println("Erro ao conectar com o ExecutionNode " + corbaname);
			System.exit(1);
		} catch (StartupFailed e) {
			System.err.println("Startup do ExecutionNode " + corbaname + "falhou.");
			System.exit(1);
		}

		//Creating container at Execution Node
		if (!this.createContainer(CONTAINER_NAME,execNode)) {
			System.err.println("Erro criando o container em " + corbaname);
			return false;
		}

		//Getting created container
		IComponent container;
		container = execNode.getContainer(CONTAINER_NAME);

		//Starting Container
		try {
			container.startup();
		} catch (StartupFailed e) {
			System.out.println("Erro no startup do container em " + corbaname);
			System.exit(1);
		}

		//Getting Component Loader Facet from container
		ComponentLoader loader = ComponentLoaderHelper.narrow(container
			.getFacet("scs::container::ComponentLoader"));
		if (loader == null) {
			System.out.println("Erro ao retornar faceta loader em " + corbaname);
			return false;
		}

		//Loading Event Manager into container
		ComponentHandle eventMgrHandle = null;
		ComponentId eventMgrCompId = new ComponentId();
		eventMgrCompId.name = "EventManager";
		eventMgrCompId.version = 1;

		eventMgrHandle = createHandle(loader, eventMgrCompId);
		if (eventMgrHandle == null) {
			return false;
		}

		IComponent eventMgr = eventMgrHandle.cmp;

		//Getting Event Factory Facet, to create event channels
		ChannelFactory chFactory = ChannelFactoryHelper.narrow(eventMgr.getFacet("scs::event_service::ChannelFactory"));
		if( chFactory== null ) {
			System.out.println("Erro ao retornar ChannelFactory!");
			return false;
		}

		//Getting Channel Collection Facet, to list created channels
		ChannelCollection chCollection = ChannelCollectionHelper.narrow(eventMgr.getFacet("scs::event_service::ChannelCollection"));
		if( chCollection == null ) {
			System.out.println("Erro ao retornar ChannelCollection!");
			return false;
		}

		IComponent masterChannel = null;

		try {
			//Creating Event Channel
			masterChannel = chFactory.create("MasterChannel");
			masterChannel.startup();

			//Getting Event Channel Facet and Receptacle
			EventSink eventSink = EventSinkHelper.narrow(masterChannel.getFacetByName("EventSink"));
			IReceptacles eventSource = IReceptaclesHelper.narrow(masterChannel.getFacet("scs::core::IReceptacles"));

			//Listing channels created by Event Manager
			ChannelDescr channels[] = chCollection.getAll();

			for (int j = 0; j < channels.length; j++) {
				ChannelDescr ch = channels[j];
				System.out.println(ch.name + ": canal criado com sucesso.");

			}
		} catch (Exception e) {
			System.out.println("Erro ao instanciar/conectar channel.");
			return false;
		}

		return true;
	}

	public static void main(String[] args) {
		int c;
		String arg = null;
		String ecHostname = "localhost";
		String ecPort = "1050";

		InputStreamReader isr = new InputStreamReader ( System.in );
		BufferedReader br = new BufferedReader ( isr );

		Getopt g = new Getopt("EventChannelApp", args, "e:p:h");

		while ((c = g.getopt()) != -1)
		{
			switch(c)
			{
				case 'e':
					arg = g.getOptarg();		            
					ecHostname = arg != null ? arg : "localhost";
					break;
				case 'p':
					arg = g.getOptarg();		            
					ecPort = arg != null ? arg : "1050";
					break;
				case 'h':
				case '?':
					System.out.println("Uso: EventChannelApp [-e hostname] [-p porta] [-h]");
					System.out.println("\t-e <hostname>\t\tHostname do Execution Node para o Event Channel (default = localhost)");
					System.out.println("\t-p <hostname>\t\tPorta do Execution Node para o Event Channel (default = 1050)");
					System.out.println("\t-h\t\t\tEssa tela de ajuda. :)");
					System.exit(1);
				break;
			}
		}

	 	System.out.println("Execution Node para o Event Channel: " + ecHostname + ":" + ecPort);

		try {
			long start = System.currentTimeMillis();
		
			EventChannelApp app = new EventChannelApp(ecHostname, ecPort);
			System.out.print("Press any key to stop monitoring ... ");
			br.readLine();

			long end = System.currentTimeMillis();
			System.out.println("Tempo total de execucao:" + (end - start));
			app.stop();
			
		} catch (IOException ioe) {
			System.err.println(ioe);
		} catch (SystemException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void stop() {
		try{
			execNode.stopContainer(CONTAINER_NAME); 
			Thread.sleep(1000);
		} catch (Exception e ) {
			System.err.println("Erro ao finalizar container");
		}
	}
}
