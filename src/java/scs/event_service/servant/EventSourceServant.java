package scs.event_service.servant;

import java.util.ArrayList;

import org.omg.CORBA.Object;

import scs.core.servant.IReceptaclesServant;
import scs.core.servant.Receptacle;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;

/**
 * Implementação das funcionalidades de EventSource do canal de eventos
 * 
 * @author Eduardo Fonseca/Luiz Marques
 *
 */
public class EventSourceServant extends IReceptaclesServant {

	private static final String RECEPTACLE_NAME  = "EventSource";
	private static final String RECEPTACLE_IFACE = "scs::event_service::EventSink";
	
	/**
	 * Channel que esta associado a este EventSource 
	 */
	EventChannelServant channel = null;	
	
	/**
	 * @param evs Servant do canal de eventos que é "dono" deste EventSource
	 */
	public EventSourceServant(EventChannelServant evs) {
		this.channel =evs;
	}	

	/* (non-Javadoc)
	 * @see SCS.servant.IReceptaclesServant#createReceptacles()
	 */
	@Override
	protected ArrayList<Receptacle> createReceptacles() {

		ArrayList<Receptacle> receptacles = new ArrayList<Receptacle>();
		Receptacle rec = new Receptacle(RECEPTACLE_NAME, RECEPTACLE_IFACE, true);
		receptacles.add(rec);
		
		return receptacles;
	}


	/* (non-Javadoc)
	 * @see SCS.servant.IReceptaclesServant#getConnectionLimit()
	 */
	@Override
	protected int getConnectionLimit() {
		return 0;	//sem limite de conexoes
	}


	/* (non-Javadoc)
	 * @see SCS.servant.IReceptaclesServant#isValidConnection(org.omg.CORBA.Object)
	 */
	@Override
	protected boolean isValidConnection(Object obj) {
		EventSink sink = EventSinkHelper.narrow(obj);
		return( sink != null );
	}

}
