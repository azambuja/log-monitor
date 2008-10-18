package scs.execution_node.servant;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.IComponent;
import scs.core.ShutdownFailed;
import scs.core.InvalidName;
import scs.event_service.servant.EventManagerServant;
import scs.execution_node.ContainerAlreadyExists;
import scs.execution_node.ContainerDescription;
import scs.execution_node.ExecutionNodePOA;
import scs.execution_node.Property;

/**
 * Servant que implementa a interface scs::execution_node::ExecutionNode
 * @author Eduardo Fonseca / Luiz Marques
*/
public class ExecutionNodeServant extends ExecutionNodePOA {

	/**
	 * Container de "containers" do SCS
	 */
	private ArrayList<ContainerDescription> containers = null;
	/**
	 * Tempo de espera para criacao do container em milissegundos 
	 */
	private long timeout = 10000; 
	
	/**
	 * Timeout default para criacao do container 
	 */
	private final static long DEFAULT_TIMEOUT = 10;	//s
	
	/**
	 * Tempo de espera entre duas verificacoes do status do container executado
	 */
	private final static int SLEEP_TIME = 500;
	
	/**
	 * Container de configuracoes do ExecutionNode
	 */
	private Properties config;
	
	/**
	 * ExecutionNodeComponent  
	 */
	private IComponent execNodeComponent;
	
	/**
	 * Referencia para o event manager instanciado para auxiliar na notificacao de metodos 
	 */
	private EventManagerServant evMgr = new EventManagerServant();
	private org.omg.CORBA.Object objEvMgr=null;

	/**
	 * Objeto que gerencia a notificacao dos containers executados
	 */
	ContainerManagerServant containerMgr = new ContainerManagerServant();
	org.omg.CORBA.Object containerMgrObj=null;
	
	
	/**
	 * @param myComponent IComponent que serve esta faceta, passado para o container
	 * @param configProp Propriedades lidas da configuracao
	 */
	public ExecutionNodeServant( IComponent myComponent, Properties configProp ) {
		containers = new ArrayList<ContainerDescription>();
		this.config = configProp;
		this.timeout = Long.parseLong(this.config.getProperty("timeout", String.valueOf(DEFAULT_TIMEOUT))) * 1000;
		this.execNodeComponent = myComponent;
	}
	
	
	/* (non-Javadoc)
	 * @see SCS.ExecutionNodeOperations#getContainer(java.lang.String)
	 */
	public IComponent getContainer(String container_name) {
		ContainerDescription cont = this.findContainer(container_name);
		if(cont!=null) {
			return cont.container;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see SCS.ExecutionNodeOperations#getContainers()
	 */
	public ContainerDescription[] getContainers() {
		return this.containers.toArray(new ContainerDescription[this.containers.size()]);
	}

	/**
	 * Metodo privado para localizar o container por nome na estrutura de containers
	 * @param name
	 * @return 
	 */
	private ContainerDescription findContainer(String name) {
		for (Iterator<ContainerDescription> iter = this.containers.iterator(); iter.hasNext();) {
			ContainerDescription cont = iter.next();
			if( cont.container_name.equals(name))
				return cont;
		}
		return null;
	}
	
	/**
	 * Executa o processo do container, passando os parametros:
	 * 	- IOR do objeto container manager
	 *  - IOR do objeto EventManager criado para notificacao de chamadas de metodos
	 *  - nome do container sendo criado
	 *  
	 *  A linha de comando para a execucao do container vem do arquivo de configuracao
	 *  
	 *  Apos a execucao do container, aguarda a notificacao atraves da interface 
	 *  ContainerManger, por um tempo definido pela variavel membro timeout
	 *  
	 * @return container
	 */
	private IComponent executeContainer(String containerName) {

		String cmdline = this.config.getProperty("container.java");
		
		if(cmdline == null) {
			System.err.println("Parameter container.java not found.");
			return null;
		}
		
		try {
			containerMgrObj = this._poa().servant_to_reference(containerMgr);
			objEvMgr = this._poa().servant_to_reference(evMgr);
		} catch (ServantNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		}

		if( containerMgrObj== null || objEvMgr==null )
			return null;

		/*
		 * Adiciona o IOR do ContainerManager criado na linha de comando do container
		 */
		String cmgrIOR = this._orb().object_to_string(containerMgrObj);
		String evMgrIOR = this._orb().object_to_string(objEvMgr);
		cmdline += " " + cmgrIOR + " " + evMgrIOR + " "+ containerName;


		IComponent container=null;
		
		synchronized (this) {
			try {
				System.out.println("Executing container: " + cmdline );
				
				Runtime.getRuntime().exec(cmdline);
				
				Thread.sleep(SLEEP_TIME);
				int elapsed = SLEEP_TIME;
				
				while( (container=containerMgr.getContainer(containerName)) == null ) {
				
					if( timeout!=0 && elapsed > this.timeout ) {
						System.err.println("Timeout for container expired, exiting ...");
						return null;
					}

					Thread.sleep(SLEEP_TIME);
					elapsed += SLEEP_TIME;
				}
			} catch (IOException e) {
				System.err.println("IOException ocurred while executing " + cmdline);
			} catch (InterruptedException e) {
				System.err.println("InterruptedException ocurred while executing " + cmdline);
			}
		}		
		
		return container;
	}
	
	/* (non-Javadoc)
	 * @see SCS.ExecutionNodeOperations#startContainer(java.lang.String, SCS.Property[])
	 */
	public IComponent startContainer(String container_name, Property[] props)
			throws ContainerAlreadyExists {

		IComponent container = null;

		if( this.findContainer(container_name) != null )
			throw new ContainerAlreadyExists();

		container = this.executeContainer(container_name);
	
		if( container == null ) {
			System.err.println("Error executing container " + container_name);
			return null;
		}
		
		ContainerDescription desc = new ContainerDescription();
		desc.container = container;
		desc.container_name = container_name;
		desc.execution_node = this.execNodeComponent;
		this.containers.add(desc);
		
		return container;
	}

        public void stopContainer(String container_name) throws InvalidName {
		ContainerDescription container = findContainer(container_name);
		if(containers.contains(container))
		{
			containers.remove(container);
			containerMgr.unregisterContainer(container_name);
		}
		try {
			container.container.shutdown();
                        System.out.println(container_name + " was stopped");
			
		} catch (ShutdownFailed e) {
			System.err.println("Error stopping container " + container_name);
			e.printStackTrace();
		}
        }

	public String getName() {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			return addr.getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return "no name";
	}

}
