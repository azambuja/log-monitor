package scs.event_service.servant;

import org.omg.CORBA.Any;

import scs.event_service.EventSinkPOA;



/**
 * Classe abstrata para facilitar a criacao de EventSinks de consumidores. 
 */
public abstract class EventSinkConsumerServant extends EventSinkPOA {

	String name;
	
	ConnectionStatus connStatus;
	
	public EventSinkConsumerServant(ConnectionStatus cs) {
		this.connStatus = cs;
	}
	
	public void push(Any ev) {
		this.handleEvent(ev);
	} 

	public void disconnect() {
		synchronized(this.connStatus) {
			this.connStatus.setConnected(false);
		}
	}

	/**
	 * Template Method para facilitar o tratamento de um EventSink de um consumidor,
	 * que so precisa implementar o corpo deste metodo.
	 */
	protected abstract void handleEvent(Any ev);
}
