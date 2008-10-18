package scs.execution_node.servant;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.net.*;


import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.UserException;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import scs.core.ComponentId;


/**
 * Classe que implementa a aplicacao do execution node do SCS.
 */
public class ExecutionNodeApp {

	static Properties config = null;
	
	static boolean readConfiguration( String filename ) {
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
	 * main: Recebe como argumento na linha de comando o host e a porta onde o 
	 * servico de nomes Corba está disponível, para localização dos componentes
	 * do execution node
	 * Caso não sejam especificados, toma como default localhost e porta 1050
	 */
	public static void main(String[] args) {
		try {
			
			if(args.length < 1) {
				System.err.println("Syntax: ExecutionNodeApp <properties-file>");
				System.exit(1);
			}
			
			if(!readConfiguration(args[0])) {
				System.err.println("Error reading configuration from file" + args[0]);
			}
			
                        Properties props = new Properties();
                        props.put("org.omg.CORBA.ORBInitialPort", config.getProperty("org.omg.CORBA.ORBInitialPort", "1050"));
                	props.put("org.omg.CORBA.ORBInitialHost", config.getProperty("org.omg.CORBA.ORBInitialHost", "localhost"));

                        System.out.println("ORB Host: " + props.getProperty("org.omg.CORBA.ORBInitialHost"));
        	        System.out.println("ORB Port "+ props.getProperty("org.omg.CORBA.ORBInitialPort") );
        	
        	        ORB orb = ORB.init(args, props);			

        	        POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
        	        poa.the_POAManager().activate();
			//System.err.println("Resolveu o RootPOA");

                        ExecutionNodeComponent servant = new ExecutionNodeComponent(config);
                        servant.createComponentId(new ComponentId("ExecutionNode", 1));

                        org.omg.CORBA.Object objNS = orb.resolve_initial_references("NameService");
                        //System.err.println("Resolveu o NameService");
                        NamingContextExt rootContext = NamingContextExtHelper.narrow( objNS );

                        //System.err.println(" Registrando execution node: ");
                        NameComponent[] nc = rootContext.to_name("ExecutionNode");

                       //System.err.println("Binding ExecutionNodeComponent servant to context ExecutionNode ...");
                       rootContext.rebind( nc, poa.servant_to_reference( servant ) );
			
                       System.err.println("Running Execution Node ...");
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
