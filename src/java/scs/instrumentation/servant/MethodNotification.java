package scs.instrumentation.servant;

import java.util.Observable;
import java.util.Observer;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.servant.OrbRunner;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;
import scs.event_service.EventSinkPOA;

/**
 * Classe que facilita o tratamento de notificacoes de metodos a partir do 
 * Interceptor de estatisticas
 * 
 * Deriva de Observable para que os clientes da notificacao possam implementar
 * a interface observer e simplificar o tratamento da notificacao.
 * 
 */
public class MethodNotification extends Observable  {

	/**
	 * Inner class que implementa um event sink que recebe a notificacao 
	 * a partir do channel correspondente e envia para os observers
	 */
	public class NotificationEventSinkServant extends EventSinkPOA {

		MethodNotification observable;
		
		private NotificationEventSinkServant(MethodNotification obs) {
			this.observable = obs;
		}
		
		public void disconnect() {
			System.out.println("NotificationEventSinkServant::disconnect()");
			this.observable.deleteObservers();
		}

		public void push(Any ev) {
			System.out.println("NotificationEventSinkServant::push() " + ev.extract_string());
			this.observable.setStatusChanged();
			this.observable.notifyObservers(ev.extract_string());
		}
	}

	NotificationEventSinkServant notifServant = null;

	EventSink eventSink = null;
	public void setStatusChanged()
	{
		System.out.println("MethodNotification::setStatusChanged()");
		this.setChanged();
	}

	/**
	 * Construtor da classe de notificacao que recebe o observer que ira
	 * ser notificado da chamada do metodo.
	 */
	public MethodNotification(Observer obs) {
		ORB orb = ORB.init(new String[]{""},null);
		POA poa = null;
		
		try {
			poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			poa.the_POAManager().activate();
		} catch (InvalidName e) {
			e.printStackTrace();
		} catch (AdapterInactive e) {
			e.printStackTrace();
		}
		
		this.addObserver(obs);
		this.notifServant = new NotificationEventSinkServant(this);
		try {
			this.eventSink = EventSinkHelper.narrow(poa.servant_to_reference(this.notifServant));
		} catch (ServantNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		}

		/*
		 * Executa o orb em outra thread para nao interromper o fluxo de execucao
		 */
		new OrbRunner(orb).start();
	}

	public EventSink getEventSink() {
		return this.eventSink;
	}
	
}
