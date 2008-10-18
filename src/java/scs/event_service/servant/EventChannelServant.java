package scs.event_service.servant;

import java.util.ArrayList;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IMetaInterface;
import scs.core.IMetaInterfaceHelper;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.servant.IComponentServant;
import scs.core.servant.IMetaInterfaceServant;
import scs.core.servant.IReceptaclesServant;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;


/**
 * Classe que contém a implementação das funcionalidades de um canal de eventos
 * @author Eduardo Fonseca/Luiz Marques
 * 
 */
public class EventChannelServant extends IComponentServant {

	private EventSink sink = null;
	private EventSinkChannelServant evSinkServant = null;
	
	private IReceptacles source = null;
	private EventSourceServant evSourceServant = null;

	FacetDescription[] facets = null;

	/**
	 * Inner class que implementa a descrição das interfaces de um canal de eventos
	 */
	private class ChannelMetaInterface extends IMetaInterfaceServant {

		IComponentServant icompServant = null; 
		
		public ChannelMetaInterface( IComponentServant servant ) {
			this.icompServant = servant;
		}
		
		@Override
		public ArrayList<IReceptaclesServant> getIReceptaclesServants() {
			ArrayList<IReceptaclesServant> result = new ArrayList<IReceptaclesServant>();
			result.add(evSourceServant);
			return result;
		}

		@Override
		public IComponentServant getIComponentServant() {
			return this.icompServant;
		}
	}
	
	private static final String FACET_EVTSINK 	= "EventSink";
	private static final String IFACE_EVTSINK 	= "scs::event_service::EventSink";
	
	private static final String FACET_EVTSOURCE 	= "EventSource";
	private static final String IFACE_EVTSOURCE 	= "scs::core::IReceptacles";

	private static final String FACET_IMETAIFACE 	= "IMetaInterface";
	private static final String IFACE_IMETAIFACE 	= "scs::core::IMetaInterface";
	
	IMetaInterface metaInterface = null;
	ChannelMetaInterface metaInterfaceServant = null;
	
	/**
	 * Retorna o EventSink, criando-o caso ainda não exista
	 * @return objeto Corba de interface EventSink
	 */
	private EventSink getEventSink() {
		if(evSinkServant == null)
		{
			this.evSinkServant = new EventSinkChannelServant(this);
			try {
				this.sink = EventSinkHelper.narrow( this._poa().servant_to_reference(this.evSinkServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return sink;
	}

	/**
	 * Retorna o EventSource, criando-o caso ainda não exista
	 * @return objeto Corba de interface EventSource
	 */
	private IReceptacles getEventSource() {
		if(this.evSourceServant == null)
		{
			this.evSourceServant= new EventSourceServant(this);
			try {
				this.source = IReceptaclesHelper.narrow( this._poa().servant_to_reference(this.evSourceServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		return source;
	}

	
	/**
	 * Retorna o IMetaInterface deste objeto, criando-o caso não exista.
	 * @return objeto Corba de interface IMetaInterface
	 */
	private IMetaInterface getMetaInterface() {
		
		if( this.metaInterfaceServant == null )
		{
			this.metaInterfaceServant = new ChannelMetaInterface(this);
			
			try {
				this.metaInterface = IMetaInterfaceHelper.narrow( this._poa().servant_to_reference(this.metaInterfaceServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		
		return this.metaInterface;
	}

	
	/* (non-Javadoc)
	 * @see SCS.servant.IComponentServant#createFacets()
	 */
	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		
		FacetDescription f = new FacetDescription();
		
		f.interface_name = IFACE_EVTSINK;
		f.name = FACET_EVTSINK;
		f.facet_ref = getEventSink();
		facets.add(f);
		
		f = new FacetDescription();
		f.interface_name = IFACE_EVTSOURCE;
		f.name = FACET_EVTSOURCE;
		f.facet_ref = getEventSource();
		facets.add(f);
		
		f = new FacetDescription();
		f.interface_name = IFACE_IMETAIFACE;
		f.name = FACET_IMETAIFACE;
		f.facet_ref = getMetaInterface();
		facets.add(f);
		
		return facets;
	}
	
	@Override
	protected boolean doShutdown() {
		return true;
	}

	@Override
	protected boolean doStartup() {
		return true;
	}

}
