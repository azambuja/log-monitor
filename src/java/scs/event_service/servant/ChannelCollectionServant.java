package scs.event_service.servant;

import java.util.ArrayList;
import java.util.Iterator;

import scs.core.IComponent;
import scs.core.InvalidName;
import scs.event_service.ChannelCollectionPOA;
import scs.event_service.ChannelDescr;
import scs.event_service.NameAlreadyInUse;


/**
 * Classe que implementa o servant da interface scs::event_service::ChannelCollection::ChannelCollection.
 * @author Eduardo Fonseca/Luiz Marques
 */
public class ChannelCollectionServant extends ChannelCollectionPOA {

	ArrayList<ChannelDescr> events = new ArrayList<ChannelDescr>();
	
	/**
	 * Adiciona um novo elemento ao container interno de EventChannels 
	 * @param name nome do novo canal
	 * @param evCh canal de eventos a ser adicionado
	 * @throws NameAlreadyInUse caso o nome do canal já exista
	 */
	public void addChannel(String name,IComponent evCh) throws NameAlreadyInUse
	{
		for (Iterator<ChannelDescr> iter = events.iterator(); iter.hasNext();) {
			ChannelDescr element = iter.next();
			if( element.name.equals(name))
				throw new NameAlreadyInUse(name);
		}
		events.add(new ChannelDescr(name,evCh));
	}
	
	/**
	 * Remove um canal existente do container interno
	 * @param name nome do canal
	 * @throws InvalidName caso o canal não exista no container
	 */
	public void removeChannel(String name) throws InvalidName
	{
		for (Iterator<ChannelDescr> iter = events.iterator(); iter.hasNext();) {
			ChannelDescr element = iter.next();
			if( element.name.equals(name))
			{
				iter.remove();
				return;
			}
		}
		
		throw new InvalidName(name);
	}
	
	/* (non-Javadoc)
	 * @see EventService.ChannelCollectionOperations#getAll()
	 */
	public ChannelDescr[] getAll() {
		return events.toArray(new ChannelDescr[events.size()]);
	}
	
	/* (non-Javadoc)
	 * @see EventService.ChannelCollectionOperations#getChannel(java.lang.String)
	 */
	public IComponent getChannel(String name) {
		for (Iterator<ChannelDescr> iter = events.iterator(); iter.hasNext();) {
			ChannelDescr element = iter.next();
			if(element.name.equals(name))
			{
				return element.channel;
			}
		}
		return null;
	}
}
