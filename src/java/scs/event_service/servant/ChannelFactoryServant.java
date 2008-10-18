package scs.event_service.servant;

import org.omg.PortableServer.POAPackage.ObjectNotActive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongAdapter;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import scs.core.IComponent;
import scs.core.IComponentHelper;
import scs.event_service.ChannelCollection;
import scs.event_service.ChannelCollectionHelper;
import scs.event_service.ChannelFactoryPOA;
import scs.event_service.InvalidName;
import scs.event_service.NameAlreadyInUse;



/**
 * Classe que implementa as funcionalidades de fábrica de canais da interface
 * scs::event_service::ChannelFactory
 * @author Eduardo Fonseca/Luiz Marques
 *
 */
public class ChannelFactoryServant extends ChannelFactoryPOA {

	ChannelCollection collection = null;
	ChannelCollectionServant ccs = null;
	
	public ChannelFactoryServant() {
		ccs = new ChannelCollectionServant();
	}
	
	/* (non-Javadoc)
	 * @see EventService.ChannelFactoryOperations#create(java.lang.String)
	 */
	public IComponent create(String name) throws NameAlreadyInUse {
		IComponent evChn = null;	
		
		try {
			EventChannelServant ecs = new EventChannelServant();

			evChn = IComponentHelper.narrow( this._poa().servant_to_reference(ecs) );
			ccs.addChannel(name, evChn);
			
		} catch (ServantNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		}
		return evChn; 
		
	}

	/* (non-Javadoc)
	 * @see EventService.ChannelFactoryOperations#destroy(java.lang.String)
	 */
	public void destroy(String name) throws InvalidName {
		IComponent evChn = collection.getChannel(name);
		try {
			ChannelCollectionServant ccs = (ChannelCollectionServant)this._poa().reference_to_servant(collection);
			ccs.removeChannel(name);
		} catch (ObjectNotActive e1) {
			e1.printStackTrace();
		} catch (WrongPolicy e1) {
			e1.printStackTrace();
		} catch (WrongAdapter e1) {
			e1.printStackTrace();
		} catch (scs.core.InvalidName e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(evChn == null)
			throw new InvalidName();
		
		try {
			this._poa().deactivate_object(this._poa().reference_to_id(evChn));
			
		} catch (ObjectNotActive e) {
			e.printStackTrace();
		} catch (WrongPolicy e) {
			e.printStackTrace();
		} catch (WrongAdapter e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * @return ChannelCollection contendo todos os canais criados até o momento 
	 */
	public ChannelCollection getCollection() {

		if( this.collection == null ) {
			try {
				this.collection = ChannelCollectionHelper.narrow( this._poa().servant_to_reference(ccs));
			} catch (ServantNotActive e) {
				e.printStackTrace();
			} catch (WrongPolicy e) {
				e.printStackTrace();
			}
		}
		
		return this.collection;
	}

}
