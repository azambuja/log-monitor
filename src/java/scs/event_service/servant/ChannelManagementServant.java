package scs.event_service.servant;

import java.util.HashMap;

import scs.core.IComponent;
import scs.core.AlreadyConnected;
import scs.core.ExceededConnectionLimit;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidConnection;
import scs.core.NoConnection;
import scs.core.StartupFailed;
import scs.event_service.ChannelCollection;
import scs.event_service.ChannelCollectionHelper;
import scs.event_service.ChannelFactory;
import scs.event_service.ChannelFactoryHelper;
import scs.event_service.EventSink;
import scs.event_service.NameAlreadyInUse;
import scs.event_service.ChannelManagementPOA;

import org.omg.CORBA.Any;

/**
 * Classe que implementa o servant da interface EventService::ChannelCollection.
 * @author Sand Luz
 */
public class ChannelManagementServant extends ChannelManagementPOA {

    /**
     * Hash para controlar as conexoes de notificacao de chamadas de metodos
     * Chave = nome do canal 
     * Valor = Connection ID 
     */
     private HashMap<String, Integer> hashConnections = null;
 
     private EventManagerServant eventMgr = null;
	
     private ChannelFactory chFactory = null;
	
     private ChannelCollection chCollection = null;

     public ChannelManagementServant(EventManagerServant eventMgr) {
          this.eventMgr = eventMgr;
          hashConnections = new HashMap<String, Integer> ();
	  
     }	  

     private IComponent getChannel(String channelName)
     {
	 if( this.chCollection == null || this.chFactory == null ) {
	      this.chFactory = ChannelFactoryHelper.narrow(this.eventMgr.getFacet("scs::event_service::ChannelFactory"));
	      if( this.chFactory== null ) {
	          System.err.println("Erro ao retornar ChannelFactory !");
		  return null;
	      }

	      this.chCollection = ChannelCollectionHelper.narrow(this.eventMgr.getFacet("scs::event_service::ChannelCollection"));
	      if( this.chCollection== null ) {
	          System.err.println("Erro ao retornar ChannelCollection !");
		  return null;
	      }
         }
		
	 //System.out.println("Channel Name: "+ channelName);
		
	 IComponent channel = this.chCollection.getChannel(channelName);
		
	 if( channel == null ) {
	    try {
	            channel = this.chFactory.create(channelName);
		    channel.startup();
	    } catch (NameAlreadyInUse e) {
		e.printStackTrace();
	        return null;
	    } catch (StartupFailed e) {
		e.printStackTrace();
		return null;
	    }
	  }
		
	  //System.out.println("Retornando channel: "+ channelName);
          return channel;
     }
     
     public boolean subscribeChannel(String clientName, String channelName, EventSink sink) {
         boolean ret = false;
         //System.out.println("subscribe( "+clientName+", "+channelName+" "+")");
		
	 IComponent channel = this.getChannel(channelName);
	
	 if( channel == null ) {
	    System.err.println("Channel null !! Erro ao retornar channel");
	    return ret;
	 }
	
	 IReceptacles evSource = IReceptaclesHelper.narrow(channel.getFacet("scs::core::IReceptacles"));
	 if( evSource == null ) {
		System.err.println("evSource == null!! Erro ao retornar faceta EventSource");
		return ret;
         }
	 try {
		 int connID = evSource.connect("EventSource", sink);
                 this.hashConnections.put(channelName, connID);
		 ret = true;
		 //System.out.println("Registrou o event sink no channel");
 		 
	 } catch (scs.core.InvalidName e) {
		e.printStackTrace();
	 } catch (InvalidConnection e) {
		e.printStackTrace();
	 } catch (AlreadyConnected e) {
		e.printStackTrace();
	 } catch (ExceededConnectionLimit e) {
		e.printStackTrace();
	 }
		
	 return ret;
    } 

    public void cancelChannel(String clientName,String channelName) {
 	IComponent channel = this.getChannel(channelName);
		
 	Integer connID = this.hashConnections.get(channelName);
	if( connID == null ) {
            return;
	}
         
        IReceptacles evSource = IReceptaclesHelper.narrow(channel.getFacet("scs::core::IReceptacles"));
	if( evSource != null ) {
	   try {
	        	evSource.disconnect(connID);
	       } catch (InvalidConnection e) {
	        	System.err.println("Excecao em cancelMethodNotification(): " + e );
	       } catch (NoConnection e) {
	        	System.err.println("Excecao em cancelMethodNotification(): " + e );
	       }
	}
     }
 
     public void notifyChannel(String channelName, Any ev)
     {
	  if( this.chCollection == null || this.chFactory == null ) 
		return;
	  
	  IComponent channel = this.chCollection.getChannel(channelName);
	  if( channel != null ) {
              EventSink evSink = ((EventSink)channel.getFacet("scs::event_service::EventSink"));
	      evSink.push(ev);
	      //System.out.println("Mandei o evento .... holy shit !!");
	  }
      }
}
