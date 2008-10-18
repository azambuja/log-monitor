package scs.demos.philosopher.servant;

import java.util.ArrayList;
import java.util.Iterator;

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
import scs.demos.philosopher.Fork;
import scs.demos.philosopher.ForkHelper;
import scs.demos.philosopher.Observer;
import scs.demos.philosopher.ObserverHelper;
import scs.demos.philosopher.Philosopher;
import scs.demos.philosopher.PhilosopherHelper;
import scs.execution_node.ContainerAlreadyExists;
import scs.execution_node.ExecutionNode;
import scs.execution_node.ExecutionNodeHelper;
import scs.execution_node.Property;
import scs.execution_node.InvalidProperty;

public class PhilosopherApp {
	
	private static final String EXEC_NODE_NAME = "ExecutionNode";
	private static final String EXEC_NODE_FACET = "scs::execution_node::ExecutionNode";
	private static final int MAX_PHILOSOPHERS = 3;
	private ExecutionNode execNode = null;
	private ArrayList<IComponent> philosophers = new ArrayList<IComponent>();
	private ArrayList<Fork> forks = new ArrayList<Fork>();
	private Observer observer;
	
	public PhilosopherApp(String[] args) {
		if( !initialize(args) )
			System.err.println("Erro iniciando a aplicacao");
	}

	/**
	 * Cria um container no ExecutionNode corrente
	 */
	private boolean createContainer(String name)
	{
		try {
			Property prop = new Property();
			prop.name="language";
			prop.value="java";
			Property propSeq[] = { prop };
			IComponent container = execNode.startContainer(name,propSeq);
			
			if( container == null ) {
				return false;
			}
			
		}catch (ContainerAlreadyExists e) {
			System.err.println("Ja existe um container com este nome.");
			return false;
		} catch (InvalidProperty e) {
			System.out.println("Erro ao setar propriedades de container ");
		    return false;
		}
		
		return true;
	}

	public static void main(String[] args)
	{
		try {
			PhilosopherApp app = new PhilosopherApp(args);
			app.run();
		} catch (SystemException ex) {
			ex.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	/**
	 * @throws InterruptedException 
	 * 
	 */
	public void run() throws InterruptedException {

		for (Iterator iter = philosophers.iterator(); iter.hasNext();) {
			IComponent phi = (IComponent) iter.next();
			Philosopher p = PhilosopherHelper.narrow(phi.getFacetByName("Philosopher"));
			p.start();
		}
		
		while( true ) {
			System.out.println("Hello from run() ...");
			Thread.sleep(5000);
		}
	}

	/**
	 * @param args
	 */
	private boolean initialize(String[] args) {
		String host = (args.length > 0) ? args[0] : "localhost";
		String port = (args.length > 1) ? args[1] : "1050";
		String corbaname = "corbaname::" + host + ":" + port + "#" + EXEC_NODE_NAME;

		ORB orb = ORB.init(args, null);

		System.out.println("Conectando ao execution node: " + corbaname);

		try {
			org.omg.CORBA.Object obj = orb.string_to_object(corbaname);
			IComponent execNodeComp = IComponentHelper.narrow(obj);
			execNodeComp.startup();
			Object ob = execNodeComp.getFacet(EXEC_NODE_FACET);
			execNode = ExecutionNodeHelper.narrow(ob);
		} catch (SystemException ex) {
			System.err.println("Erro ao conectar com o ExecutionNode");
			System.exit(1);
		} catch (StartupFailed e) {
			System.err.println("Startup do ExecutionNode falhou.");
			System.exit(1);
		}

		
		String containerName = "DemoContainer";
		if( !this.createContainer(containerName) )
		{
			System.err.println("Erro criando o container !!");
			return false;
		}
		
		IComponent container;
		container = execNode.getContainer(containerName);

		try {
			container.startup();
		} catch (StartupFailed e1) {
			System.out.println("Erro no startup do container");
			System.exit(1);
		}

		
		ComponentLoader loader = ComponentLoaderHelper.narrow(container.getFacet("scs::container::ComponentLoader"));
		if( loader == null ) {
			System.out.println("component loader retornado == null !!");
			return false;
		}

		Object componentCollection = container.getFacet("scs::container::ComponentCollection");
		ComponentCollection components = ComponentCollectionHelper.narrow(componentCollection);
		
		ComponentId phiCompId = new ComponentId();
		phiCompId.name = "Philosopher";
		phiCompId.version = 1;
		
		ComponentId forkCompId = new ComponentId();
		forkCompId.name = "Fork";
		forkCompId.version = 1;
		
		ComponentId obsCompId = new ComponentId();
		obsCompId.name = "Observer";
		obsCompId.version = 1;
		
		ComponentHandle phiHandle = null;
		ComponentHandle forkHandle = null;
		ComponentHandle obsHandle = null;


		try {
			obsHandle = loader.load(obsCompId,new String[]{""});
			obsHandle.cmp.startup();
		} catch (ComponentNotFound e1) {
			e1.printStackTrace();
			return false;
		} catch (ComponentAlreadyLoaded e1) {
			ComponentHandle handles[] = components.getComponent(obsCompId);
			obsHandle = handles[0];
		} catch (LoadFailure e1) {
			e1.printStackTrace();
			return false;
		} catch (StartupFailed e) {
			e.printStackTrace();
		}

		
		this.observer = ObserverHelper.narrow(obsHandle.cmp.getFacetByName("Observer"));
		try {
			obsHandle.cmp.startup();
		} catch (StartupFailed e2) {
			e2.printStackTrace();
		}
		
		
		for( int i = 0 ; i < MAX_PHILOSOPHERS ; i++ ) {
			try {
				phiHandle = loader.load(phiCompId,new String[]{""});
			} catch (ComponentNotFound e1) {
				e1.printStackTrace();
				return false;
			} catch (ComponentAlreadyLoaded e1) {
				ComponentHandle handles[] = components.getComponent(phiCompId);
				phiHandle = handles[0];
			} catch (LoadFailure e1) {
				e1.printStackTrace();
				return false;
			}

			try {
				forkHandle = loader.load(forkCompId,new String[]{""});
			} catch (ComponentNotFound e1) {
				e1.printStackTrace();
				return false;
			} catch (ComponentAlreadyLoaded e1) {
				ComponentHandle handles[] = components.getComponent(forkCompId);
				forkHandle = handles[0];
			} catch (LoadFailure e1) {
				e1.printStackTrace();
				return false;
			}

			try {
				phiHandle.cmp.startup();
				forkHandle.cmp.startup();
			} catch (StartupFailed e) {
				e.printStackTrace();
				return false;
			}
			
			Philosopher p = PhilosopherHelper.narrow(phiHandle.cmp.getFacetByName("Philosopher"));
			p.setName("Philosopher " + i);
			
			System.out.println("Philosopher " + i + " carregado com sucesso.");
			Fork f = ForkHelper.narrow(forkHandle.cmp.getFacetByName("Fork"));
			
			this.philosophers.add(phiHandle.cmp);
			this.forks.add(f);
			
			
			IReceptacles info = IReceptaclesHelper.narrow(phiHandle.cmp.getFacetByName("info"));
			
			Object facet = phiHandle.cmp.getFacetByName("right");
			IReceptacles right = IReceptaclesHelper.narrow(facet);
			
			try {
				info.connect("info", this.observer);

				right.connect("right", f);

				facet = phiHandle.cmp.getFacetByName("left");
				IReceptacles left = IReceptaclesHelper.narrow(facet);

				if( i > 0 )
					left.connect("left", forks.get(i-1));
				
				if( i == MAX_PHILOSOPHERS-1 )
				{
					facet = this.philosophers.get(0).getFacetByName("left");
					left = IReceptaclesHelper.narrow(facet);
					left.connect( "left", forks.get(i) );
				}
				 
			} catch (InvalidName e) {
				e.printStackTrace();
			} catch (InvalidConnection e) {
				e.printStackTrace();
			} catch (AlreadyConnected e) {
				e.printStackTrace();
			} catch (ExceededConnectionLimit e) {
				e.printStackTrace();
			}
			
		}
		
		return true;
	}
}
