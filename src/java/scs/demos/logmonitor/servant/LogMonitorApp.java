package scs.demos.logmonitor.servant;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;
import org.omg.CORBA.SystemException;

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
import scs.demos.logmonitor.LogMonitor;
import scs.demos.logmonitor.LogMonitorHelper;
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
	private IComponent logMonitorComponent1 = null;
	private IComponent logMonitorComponent2 = null;

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

	/**
		* @param args
		*/
	private boolean initialize(String[] args) {
		String host= null;
		String port = null;
		String corbaname = null;
		int id = 1;
		int numComponentPerNode;

/*		if (args.length > 0 && args.length < 4) { 
			System.out.println("Para criar os componentes em maquinas diferentes:");
			System.out.println("   Syntax: PingPongApp <host-1> <port-1> <host-2> <port-2>");
			System.exit(1);
		}
*/

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

			Object componentCollection = container
				.getFacet("scs::container::ComponentCollection");
			ComponentCollection components = ComponentCollectionHelper
				.narrow(componentCollection);

			ComponentId logMonitorCompId = new ComponentId();
			logMonitorCompId.name = "LogMonitor";
			logMonitorCompId.version = 1;

			ComponentHandle logMonitorHandle = null;

			for (int j=0; j < numComponentPerNode; j++) {
				try {
					logMonitorHandle = loader.load(logMonitorCompId, new String[] { "" });
				} catch (ComponentNotFound e) {
					System.err.println("COMPONENTE NAO ENCONTRADO !");
					e.printStackTrace();
					return false;
				} catch (ComponentAlreadyLoaded e) {
					ComponentHandle handles[] = components.getComponent(logMonitorCompId);
					logMonitorHandle = handles[0];
				} catch (LoadFailure e) {
					e.printStackTrace();
					return false;
				}

				try {
					logMonitorHandle.cmp.startup();
				} catch (StartupFailed e) {
					e.printStackTrace();
					return false;
				}

				LogMonitor log = LogMonitorHelper.narrow(logMonitorHandle.cmp
					.getFacetByName("LogMonitor"));

				log.setId(id);
				System.out.println("LogMonitor id =" + id + " carregado com sucesso.");

				if (id ==1)
					logMonitorComponent1 = logMonitorHandle.cmp;
				else
					logMonitorComponent2 = logMonitorHandle.cmp;

				id++;	
			}
		}

		IReceptacles info1 = IReceptaclesHelper.narrow(logMonitorComponent1
			.getFacetByName("infoReceptacle"));
		IReceptacles info2 = IReceptaclesHelper.narrow(logMonitorComponent2
			.getFacetByName("infoReceptacle"));

		LogMonitor log1 = LogMonitorHelper.narrow(logMonitorComponent1
			.getFacetByName("LogMonitor"));
		LogMonitor log2 = LogMonitorHelper.narrow(logMonitorComponent2
			.getFacetByName("LogMonitor"));

		try {
			info1.connect("LogMonitor", log1);
			info2.connect("LogMonitor", log2);
		} catch (InvalidName e) {
			e.printStackTrace();
		} catch (InvalidConnection e) {
			e.printStackTrace();
		} catch (AlreadyConnected e) {
			e.printStackTrace();
		} catch (ExceededConnectionLimit e) {
			e.printStackTrace();
		}
		return true;
	}

	public static void main(String[] args) {
		try {
			long start = System.currentTimeMillis();
			LogMonitorApp app = new LogMonitorApp(args);
			app.run();
			app.stop();
			long end = System.currentTimeMillis();
			System.out.println("Tempo total de execucao:" + (end - start));
		} catch (SystemException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	/**
		* * @throws InterruptedException
		* * 
		* */
	public void run() throws InterruptedException {
		LogMonitor log1 = LogMonitorHelper.narrow(logMonitorComponent1
			.getFacetByName("LogMonitor"));
		LogMonitor log2 = LogMonitorHelper.narrow(logMonitorComponent2
			.getFacetByName("LogMonitor"));

		log2.start();
		log1.start();

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
