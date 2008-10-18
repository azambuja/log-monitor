package scs.container.servant;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UserException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.execution_node.ContainerManager;
import scs.execution_node.ContainerManagerHelper;
import scs.core.ComponentId;

/**
 * Classe que implementa a aplicacao do Container do SCS.
 */
public class ContainerApp {
	
	private static Properties config = null;

	/**
	 * @param filename
	 * @return boolean indicando se a operação de leitura de configuracoes foi completada
	 */
	private static boolean readConfiguration( String filename ) {
	    Properties properties = new Properties();
	    
	    try {
	        properties.load(new FileInputStream(filename));
	    } catch (IOException e) {
	    	System.err.println(e);
	    	return false;
	    }

	    config = properties;
	    return true;
	}

	
	/**
	 * @param args Argumentos da main:
	 * - nome do arquivo de configuracao
	 * - ior do objeto ContainerParent criado no execution node
	 * - ior do EventManager criado no execution node
	 * - nome do container
	 */
	public static void main(String[] args) {

		if( args.length != 4 ) {
			System.out.println("Error: incorrect parameters.");
			System.out.println("Syntax: ContainerApp <configuration-file> <ior-container-parent> <ior-event-mgr> <container-name>");
			System.out.println("<configuration-file> = name of properties file containing configuration items");
			System.out.println("<ior-container-parent> = IOR for ContainerParent object from Execution Node" );
			System.out.println("<ior-event-mgr> = IOR for EventManager from Execution Node" ); 
			System.out.println("<container-name> = Name for this container");
			System.exit(1);     
		}
		
		String configFile = args[0];
		String iorParent = args[1];
		String iorEvMgr = args[2];
		String containerName = args[3];
		
		try {
			if( !readConfiguration(configFile) ) {
				System.err.println("Error reading configuration file.");
				System.exit(1);
			}
			
			ORB orb = ORB.init(args, config);
			
			POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			poa.the_POAManager().activate();

			IComponent eventMgr = null;
			if (!iorEvMgr.equals("null"))
			{
				eventMgr = IComponentHelper.narrow(orb.string_to_object( iorEvMgr ));
			}
			if( eventMgr != null ) {
				eventMgr.startup();
			}

			ContainerServant containerServant = ContainerServant.getInstance();
			containerServant.initialize( containerName, config , eventMgr );
			containerServant.createComponentId(new ComponentId("ContainerServant", 1));
			
			org.omg.CORBA.Object container = poa.servant_to_reference(containerServant);

//			System.err.println("Recebeu IOR do container parent: " + iorParent );
			
	        org.omg.CORBA.Object objParent = orb.string_to_object( iorParent );
	        ContainerManager cmgr = ContainerManagerHelper.narrow(objParent);
	        
//			System.err.println("Notificando o container parent: IOR do container = " + container.toString());
	        cmgr.registerContainer(containerName,IComponentHelper.narrow(container));
	        
	        System.err.println("Running container ...." );
			orb.run();
		}
		catch (UserException e) {
			e.printStackTrace();
		}
		catch (SystemException e) {
			e.printStackTrace();
		}
		
	}

}
