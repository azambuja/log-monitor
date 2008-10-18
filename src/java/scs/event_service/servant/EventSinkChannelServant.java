package scs.event_service.servant;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.omg.CORBA.Any;

import scs.core.ConnectionDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.event_service.EventSink;
import scs.event_service.EventSinkPOA;



/**
 * Classe que contém as funcionalidades da interface EventSink suportada pelo
 * canal de eventos.
 * 
 * @author Eduardo Fonseca/Luiz Marques
 *
 */
public class EventSinkChannelServant extends EventSinkPOA {
	
	/**
	 * Inner class responsavel por enviar assincronamente os 
	 * eventos para os consumidores. Consiste basicamente de
	 * uma thread que fica sempre tentando ler de uma fila de
	 * eventos aqueles que estão pendentes para envio.
	 */
	private class EventDispatcher extends Thread {

		BlockingQueue<Any> eventQueue = null;
		EventChannelServant channel = null;
		
		/**
		 * @param channel canal que é "pai" deste dispatcher
		 * @param queue fila que contém os eventos prontos para serem enviados
		 */
		public EventDispatcher( EventChannelServant channel, BlockingQueue<Any> queue ) {
			this.eventQueue = queue;
			this.channel = channel;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {

			while(true){
				
				Any event;
				
				try {
					/*
					 * Remove um elemento da fila de forma bloqueante
					 * Só acorda quando tiver um elemento ou quando a aplicação
					 * estiver terminando 
					 */
					event = this.eventQueue.take();
				} catch (InterruptedException e) {
					System.err.println("Dispatcher interrupted ! Exiting ...");
					break;
				}

				synchronized(this)
				{
					IReceptacles source = IReceptaclesHelper.narrow(this.channel.getFacet("scs::core::IReceptacles"));
					
					ConnectionDescription[] connections=null;
					
					try {
						connections = source.getConnections("EventSource");
					} catch (scs.core.InvalidName e) {
						e.printStackTrace();
					}
					

					/*
					 * Tenta despachar para todos os EventSinks registrados no EventSource
					 */
					for (int i = 0; i < connections.length; i++) {
						ConnectionDescription description = connections[i];
						((EventSink)description.objref).push(event);
						//System.out.println("Dispatcher despachou evento para o destino apropriado");
					}
				}
			}
		}
	}
	

	
	private EventChannelServant evtChn = null;
	
	private BlockingQueue<Any> eventQueue = null;
	
	private EventDispatcher eventDisp = null;
	
	/*
	 * Construtor recebe o event channel associado ao EventSink
	 */
	public EventSinkChannelServant(EventChannelServant ev) {
		this.evtChn = ev;
		this.eventQueue = new LinkedBlockingQueue<Any>();
		this.eventDisp = new EventDispatcher(this.evtChn, this.eventQueue);
		this.eventDisp.start();
	}
	
	/*
	 *  (non-Javadoc)
	 * @see EventService.EventSinkOperations#disconnect()
	 */
	public void disconnect() {

   		IReceptacles source = IReceptaclesHelper.narrow(this.evtChn.getFacet("scs::core::IReceptacles"));
		ConnectionDescription[] connections=null;
		try {
			connections = source.getConnections("EventSource");
		} catch (scs.core.InvalidName e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < connections.length; i++) {
			ConnectionDescription description = connections[i];
			((EventSink)description.objref).disconnect();
		}
	}

	/*
	 *  (non-Javadoc)
	 * @see EventService.EventSinkOperations#push(org.omg.CORBA.Any)
	 */
	public void push(Any event) {
		//String s = event.extract_string();
		//System.out.println("EventSinkChannelServant.push: " + s);
		this.eventQueue.offer(event);
		//System.out.println("EventSinkChannelServant.push: evento enfileirado");
	}

}
