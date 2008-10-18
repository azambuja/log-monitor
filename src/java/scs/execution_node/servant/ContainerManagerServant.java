package scs.execution_node.servant;

import java.util.HashMap;

import scs.core.IComponent;
import scs.core.InvalidName;
import scs.execution_node.ContainerAlreadyExists;
import scs.execution_node.ContainerManagerPOA;
import scs.execution_node.InvalidContainer;


/**
 * Servant da interface ContainerManager, para permitir a notificacao 
 * do container ao seu pai (execution node)
 * 
 * Este objeto deve ser instanciado e servido pelo ExecutionNode, que
 * passa o seu IOR pela linha de comando para o container.
 * 
 * O processo do container recebe o IOR, referencia este objeto e notifica
 * que esta pronto para ser usado para o ExecutionNode, atraves desta interface.
 *  
 * @author Eduardo Fonseca / Luiz Marques
 */
public class ContainerManagerServant extends ContainerManagerPOA {

	private HashMap<String,IComponent> containers = new HashMap<String, IComponent>();
	
	public IComponent getContainer(String cname) {
		return this.containers.get(cname);
	}

	public void registerContainer(String name, IComponent ctr) throws ContainerAlreadyExists, InvalidContainer {
		if( containers.put(name, ctr) != null )
			throw new ContainerAlreadyExists();
	}

	public void unregisterContainer(String name) throws InvalidName {
		if( containers.remove(name) == null )
			throw new InvalidName();
	}
	
}
