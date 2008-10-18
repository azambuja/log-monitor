package scs.event_service.servant;

import java.util.ArrayList;

import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.ComponentId;
import scs.core.FacetDescription;
import scs.core.IMetaInterface;
import scs.core.IMetaInterfaceHelper;
import scs.core.ShutdownFailed;
import scs.core.servant.IComponentServant;
import scs.core.servant.IMetaInterfaceServant;
import scs.core.servant.IReceptaclesServant;
import scs.event_service.ChannelCollection;
import scs.event_service.ChannelDescr;
import scs.event_service.ChannelFactory;
import scs.event_service.ChannelFactoryHelper;
import scs.event_service.ChannelManagement;
import scs.event_service.ChannelManagementHelper;
import scs.event_service.EventSink;
import scs.event_service.EventSinkHelper;


/**
 * @author Eduardo Fonseca/Luiz Marques
 *
 */
public class EventManagerServant extends IComponentServant {

	/**
	 * Inner class contendo a descricao das interfaces de EventManager
	 */
	private class EvMgrMetaInterface extends IMetaInterfaceServant {

		IComponentServant icompServant = null;
		
		public EvMgrMetaInterface(IComponentServant ics) {
			this.icompServant = ics;
		}
		
		@Override
		public IComponentServant getIComponentServant() {
			return this.icompServant;
		}

		@Override
		public ArrayList<IReceptaclesServant> getIReceptaclesServants() {
			return null;
		}

	}

	private static final String FACET_CHFACTORY 	= "ChannelFactory";
	private static final String IFACE_CHFACTORY 	= "scs::event_service::ChannelFactory";

	private static final String FACET_CHCOLLECTION 	= "ChannelCollection";
	private static final String IFACE_CHCOLLECTION 	= "scs::event_service::ChannelCollection";

        private static final String FACET_CHMANAGEMENT 	= "ChannelManagement";
	private static final String IFACE_CHMANAGEMENT 	= "scs::event_service::ChannelManagement";


	private static final String FACET_IMETAIFACE 	= "IMetaInterface";
	private static final String IFACE_IMETAIFACE 	= "scs::core::IMetaInterface";
	
	ChannelFactoryServant factoryServant = null;
	ChannelFactory factory = null;

        ChannelManagementServant managementServant = null;
	ChannelManagement management = null;
	
	EvMgrMetaInterface metaInterfaceServant = null;
	IMetaInterface metaInterface = null;


	/**
	 * @return Collection contento todos os canais já criados
	 */
	private ChannelCollection getCollection() {
		
		return this.factoryServant.getCollection();
	}

	/**
	 * @return Fábrica de canais, para a criação de novos
	 */
	private ChannelFactory getFactory() {
		
		if( this.factoryServant == null )
		{
			factoryServant = new ChannelFactoryServant(); 
		
			try {
				this.factory = ChannelFactoryHelper.narrow( this._poa().servant_to_reference(factoryServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		
		return this.factory;
	}


        /**
	 * @return Gerente de Canal para subscrição, notificação e cancelamento
	 */
	private ChannelManagement getManagement() {
		
		if( this.managementServant == null )
		{
			managementServant = new ChannelManagementServant(this); 
		
			try {
				this.management = ChannelManagementHelper.narrow( this._poa().servant_to_reference(managementServant));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		
		return this.management;
	}


		
	/**
	 * @return Objeto que contém a descrição de todas as facetas desta classe
	 */
	private IMetaInterface getMetaInterface() {
		
		if( this.metaInterfaceServant == null )
		{
			this.metaInterfaceServant = new EvMgrMetaInterface(this);
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
	 * @see SCS.servant.IComponentServant#createComponentId()
	 *
	 */
	protected ComponentId createComponentId() {
		return new ComponentId("EventManagerServant", 1);
	}

	/* (non-Javadoc)
	 * @see SCS.servant.IComponentServant#createFacets()
	 */
	@Override
	protected ArrayList<FacetDescription> createFacets() {
		ArrayList<FacetDescription> facets = new ArrayList<FacetDescription>();
		
		FacetDescription fd = new FacetDescription();
		fd.name = FACET_CHFACTORY;
		fd.interface_name= IFACE_CHFACTORY;
		fd.facet_ref = this.getFactory();
		facets.add(fd);
		
		fd = new FacetDescription();
		fd.name = FACET_CHCOLLECTION;
		fd.interface_name= IFACE_CHCOLLECTION;
		fd.facet_ref = this.getCollection();
		facets.add(fd);

                fd = new FacetDescription();
		fd.name = FACET_CHMANAGEMENT;
		fd.interface_name= IFACE_CHMANAGEMENT;
		fd.facet_ref = this.getManagement();
		facets.add(fd);

		
		fd = new FacetDescription();
		fd.name = FACET_IMETAIFACE;
		fd.interface_name= IFACE_IMETAIFACE;
		fd.facet_ref = this.getMetaInterface();
		facets.add(fd);
		
		return facets;
	}

	@Override
	protected boolean doShutdown() {

		System.out.println("Shutdown do EventManager");
		if(this.factoryServant == null) return true;
			
		ChannelDescr channels[] = this.getCollection().getAll();
		
		for (int i = 0; i < channels.length; i++) {
			ChannelDescr ch = channels[i];
			EventSink sink = EventSinkHelper.narrow(ch.channel.getFacet("scs::event_service::EventSink"));

			if( sink != null )
				sink.disconnect();	
			
			try {
				ch.channel.shutdown();
			} catch (ShutdownFailed e) {
				e.printStackTrace();
			}
			
		}
		
		return true;
	}

	@Override
	protected boolean doStartup() {
		return true;
	}

}
