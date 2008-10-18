package scs.core.servant;

import java.util.ArrayList;
import java.util.Iterator;

import scs.core.AlreadyConnected;
import scs.core.ConnectionDescription;
import scs.core.ExceededConnectionLimit;
import scs.core.ReceptacleDescription;

/**
 * Classe criada para facilitar o gerenciamento dos receptaculos no servant 
 * da interface IReceptacles.
 * 
 * @author Eduardo Fonseca e Luiz Marques
 */
public class Receptacle {

    String name;
    
    String interfaceName;
    
    boolean isMultiplex;
    
    ArrayList<ConnectionDescription> connections;
    
    /**
     * @param name
     * @param interfaceName
     * @param isMultiplex
     */
    public Receptacle(String name, String interfaceName, boolean isMultiplex) {
    	this.name = name;
    	this.interfaceName = interfaceName;
    	this.isMultiplex = isMultiplex;
    	connections = new ArrayList<ConnectionDescription>();
    }

	public String getName() {
		return name;
	}

	public String getInterfaceName() {
		return interfaceName;
	}

	public boolean isMultiplex() {
		return isMultiplex;
	}

	public ArrayList<ConnectionDescription> getConnections() {
		return connections;
	}

	public ConnectionDescription getConnection(int id) {
		for (Iterator<ConnectionDescription> iter = this.connections.iterator(); iter.hasNext();) {
			ConnectionDescription conn = iter.next();
			if( conn.id == id )
				return conn;
		}
		return null;
	}

	/**
	 * @param obj
	 * @return
	 * @throws AlreadyConnected
	 * @throws ExceededConnectionLimit 
	 */
	public int addConnection(int id, org.omg.CORBA.Object obj) throws AlreadyConnected {

		for (Iterator<ConnectionDescription> iter = this.connections.iterator(); iter.hasNext();) {
			ConnectionDescription conn = iter.next();
			if( conn.objref == obj )
				throw new AlreadyConnected();
		}
		
		ConnectionDescription conn = new ConnectionDescription();
		conn.id = id;
		conn.objref = obj;
		this.connections.add(conn);
		
		return conn.id;
	}

	/**
	 * @param id
	 */
	public void removeConnection(int id) {
		for (Iterator<ConnectionDescription> iter = this.connections.iterator(); iter.hasNext();) {
			ConnectionDescription conn = iter.next();
			if( conn.id == id )
				iter.remove();
		}
	}
	
	/**
	 * @return
	 */
	public ReceptacleDescription getReceptacleDescription() {
		ReceptacleDescription desc = new ReceptacleDescription();
		desc.name = this.getName();
		desc.interface_name = this.getInterfaceName();
		desc.is_multiplex = this.isMultiplex();
		desc.connections = this.connections.toArray(new ConnectionDescription[this.connections.size()]);
		return desc;
	}
}
