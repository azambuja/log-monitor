package scs.core.servant;

import java.util.ArrayList;
import java.util.Iterator;

import scs.core.AlreadyConnected;
import scs.core.ConnectionDescription;
import scs.core.ExceededConnectionLimit;
import scs.core.IReceptaclesPOA;
import scs.core.InvalidConnection;
import scs.core.InvalidName;
import scs.core.NoConnection;
import scs.core.ReceptacleDescription;

/**
 * Classe abstrata que implementa as caracteristicas comuns aos IReceptacles 
 * do SCS.
 */
public abstract class IReceptaclesServant extends IReceptaclesPOA {

	ArrayList<Receptacle> receptacles = null;

	/**
	 * Contador para gerar o ID da conexao (por instancia) 
	 */
	int connectionCounter = 0;

	/**
	 * Limite de conexoes 
	 */
	int connectionLimit = 0;

	
	/**
	 * ctor default
	 */
	public IReceptaclesServant() {
	}
	
	/**
	 * Metodo que retorna o conjunto de receptaculos das classes derivadas 
	 */
	protected abstract ArrayList<Receptacle> createReceptacles();

	/**
	 * Metodo abstrato para as classes concretas implementarem o limite de conexoes
	 * @return limite de conexoes 
	 */
	protected abstract int getConnectionLimit();

	/**
	 * Metodo abstrato para as classes derivadas validarem as conexoes
	 * @return status da conexao
	 */
	protected abstract boolean isValidConnection(org.omg.CORBA.Object obj);
	
	/**
	 * @param name
	 * @return
	 */
	protected Receptacle findReceptacle(String name) {

		if( this.receptacles == null )
			this.receptacles = this.createReceptacles();

		for (Iterator<Receptacle> iter = this.receptacles.iterator(); iter.hasNext();) {
			Receptacle rec = iter.next();
			if( rec.getName().equals(name) )
				return rec;
		}
		
		return null;
	}

	/**
	 * @param connId
	 * @return
	 */
	protected Receptacle findReceptacleByConnection(int connId) {
		
		if( this.receptacles == null )
			this.receptacles = this.createReceptacles();

		for (Iterator<Receptacle> iter = this.receptacles.iterator(); iter.hasNext();) {
			Receptacle rec = iter.next();
			ConnectionDescription conn = rec.getConnection(connId);
			if( conn != null )
				return rec;
		}
		
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see SCS.IReceptaclesOperations#connect(java.lang.String, org.omg.CORBA.Object)
	 */
	public int connect(String receptacle, org.omg.CORBA.Object obj) throws InvalidName, InvalidConnection, AlreadyConnected, ExceededConnectionLimit {

		if( this.receptacles == null )
			this.receptacles = this.createReceptacles();

		if( this.connectionLimit != 0 && this.connectionCounter >= this.connectionLimit )
			throw new ExceededConnectionLimit();
		
		Receptacle rec = this.findReceptacle(receptacle);
		
		if( rec == null )
			throw new InvalidName();
		
		if( !isValidConnection(obj) )
			throw new InvalidConnection();
		
		return rec.addConnection(++this.connectionCounter, obj);
	}

	/* (non-Javadoc)
	 * @see SCS.IReceptaclesOperations#disconnect(int)
	 */
	public void disconnect(int id) throws InvalidConnection, NoConnection {
		if(id < 0)
			throw new InvalidConnection();

		Receptacle rec = this.findReceptacleByConnection(id);
		if( rec == null )
			throw new NoConnection();
		
		rec.removeConnection(id);
	}

	/* (non-Javadoc)
	 * @see SCS.IReceptaclesOperations#getConnections(java.lang.String)
	 */
	public ConnectionDescription[] getConnections(String receptacle) throws InvalidName {
		if( this.receptacles == null )
			this.receptacles = this.createReceptacles();

		ArrayList<ConnectionDescription> connections = new ArrayList<ConnectionDescription>();
		
		for (Iterator<Receptacle> iter = this.receptacles.iterator(); iter.hasNext();) {
			Receptacle rec = iter.next();
			connections.addAll(rec.getConnections());
		}

		return connections.toArray(new ConnectionDescription[connections.size()]);
	}

	/**
	 * @return
	 */
	public ArrayList<ReceptacleDescription> getReceptacles() {

		if( this.receptacles == null ) {
			this.receptacles = this.createReceptacles();
		}
		
		ArrayList<ReceptacleDescription> recDesc = new ArrayList<ReceptacleDescription>();

		for (Iterator<Receptacle> iter = this.receptacles.iterator(); iter.hasNext();) {
			Receptacle rec = iter.next();
			recDesc.add(rec.getReceptacleDescription());
		}

		return recDesc;
	}

}
