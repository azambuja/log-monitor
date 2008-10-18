package scs.instrumentation.servant;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

import scs.container.servant.ContainerServant;
import scs.core.AlreadyConnected;
import scs.core.ExceededConnectionLimit;
import scs.core.IComponent;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidConnection;
import scs.core.NoConnection;
import scs.core.StartupFailed;
import scs.event_service.ChannelCollection;
import scs.event_service.ChannelCollectionHelper;
import scs.event_service.ChannelFactory;
import scs.event_service.ChannelFactoryHelper;
import scs.event_service.ChannelManagement;
import scs.event_service.ChannelManagementHelper;
import scs.event_service.EventSink;
import scs.event_service.NameAlreadyInUse;
import scs.instrumentation.InterfaceStats;
import scs.instrumentation.ContainerStats;
import scs.instrumentation.ContainerStatsHelper;
import scs.instrumentation.MethodStats;
import scs.instrumentation.StatsCollectionPOA;
import scs.instrumentation.app.LogSender;
import scs.instrumentation.ComponentStatsNotAvailable;
import scs.instrumentation.InterfaceStatsNotAvailable;
import scs.instrumentation.IOStatsNotAvailable;
import scs.instrumentation.NetworkStatsNotAvailable;
import scs.instrumentation.SystemIOStats;
import scs.instrumentation.SystemIOStatsHelper;
import scs.instrumentation.SystemNetworkStats;
import scs.instrumentation.SystemNetworkStatsHelper;
import system.SystemInformation;
import system.SystemInformation.IOStats;
import system.SystemInformation.NetworkStats;
 
/**
 * Classe que implementa o servant da interface scs::instrumentation::StatsCollection.
 * Funciona como um repositorio de estatisticas do container em que este classe é
 * executada.
 * 
 * Implementa o padrao singleton, pois pode ser usada do Interceptor.
 * 
 * @author Eduardo Fonseca/Luiz Marques
 */
public class StatsCollectionServant extends StatsCollectionPOA {

	private static StatsCollectionServant _instance = null;

	IComponent eventMgr = null;
        
        ChannelManagement chManagement = null;
	
		
	/**
	 * Hash para controlar as conexoes de notificacao de chamadas de metodos
	 * Chave = nome da interface + metodo
	 * Valor = Connection ID 
	 */
	HashMap<String, Integer> hashConnections = null;
	
	public static StatsCollectionServant getInstance()
	{
		if(_instance == null)
		{
			_instance = new StatsCollectionServant(); 
		}
		return _instance;
	}
	
	ArrayList<InterfaceStats> componentsStats = new ArrayList<InterfaceStats>();

	private ContainerStats containerStats;
        private SystemIOStats  systemIOStats;
        private SystemNetworkStats systemNetworkStats;  
	
	private StatsCollectionServant()
	{
		containerStats = new ContainerStats();
                systemIOStats = new SystemIOStats();
                systemNetworkStats = new SystemNetworkStats();
		containerStats.containerName = "";
		hashConnections = new HashMap<String, Integer> ();
        }

	/**
	 * Monta o nome do canal, concatenando o nome do container, o nome da interface
	 * e o nome do metodo (separados por |).
	 */
	private String getChannelName( String iface, String method ) {
		return( ContainerServant.getInstance().getName() 
			+ "|" + iface 
			+ "|" + method );
	}
        
	/**
	 * Monta o nome do canal usando o nome do container
	 */
         private String getChannelName(){
            return( ContainerServant.getInstance().getName());
        }
         
       	/**
	 * Retorna a estrutura contendo a estatísticaso nome do canal, concatenando o nome do container, o nome da interface
	 * e o nome do metodo (separados por |).
	 */
         private MethodStats find(String interfaceName , String methodName )
	{
		MethodStats[] msArray =this.findByInterface(interfaceName);
		if(msArray == null)
		{
			InterfaceStats cs = new InterfaceStats();
			cs.interfaceName = interfaceName;
			cs.methodStatsCollection = new MethodStats[1];
			MethodStats newMs = new MethodStats();
			newMs.callsCount = 0;
			newMs.cpuTime = 0;
			newMs.elapsedTime = 0;
			newMs.methodName = methodName;
			cs.methodStatsCollection[0] = newMs;
			componentsStats.add(cs);
			return newMs;
		}
		MethodStats ms = findByMetod(msArray, methodName);
		if(ms == null)
		{
			msArray = insertIntoArray(msArray, methodName);
			for (Iterator iter = componentsStats.iterator(); iter.hasNext();) {
				InterfaceStats element = (InterfaceStats) iter.next();
				if(element.interfaceName.equalsIgnoreCase(interfaceName))
				{
					element.methodStatsCollection = msArray;
				}
			}
			return msArray[msArray.length -1];
		}
		return ms;
	}

	private MethodStats[] insertIntoArray(MethodStats[] msArray, String methodName)
	{
		MethodStats[] newMs = new MethodStats[msArray.length + 1];
		System.arraycopy(msArray, 0, newMs, 0, msArray.length);
		MethodStats ms = new MethodStats();
		ms.callsCount = 0;
		ms.cpuTime = 0;
		ms.elapsedTime = 0;
		ms.methodName = methodName;
		newMs[newMs.length - 1] = ms;
		
		return newMs;
	}
	
	private MethodStats[] findByInterface(String interfaceName)
	{
		for (Iterator iter = componentsStats.iterator(); iter.hasNext();) {
			InterfaceStats element = (InterfaceStats) iter.next();
			
			if(element.interfaceName == null ) {
				System.err.println("StatsCollectionServant: if(element.interfaceName == null ");
			}
			
			if(element.interfaceName.equalsIgnoreCase(interfaceName))
			{
				return element.methodStatsCollection;
			}
		}
		return null;
	}
	
	private MethodStats findByMetod(MethodStats[] stats, String methodName)
	{
		for (int i = 0; i < stats.length; i++) {
			if(((MethodStats)stats[i]).methodName.equals(methodName))
			{
				return stats[i];
			}
		}
		return null;
	}

        private String getHost(){
            String host = "";
	    try {
	            InetAddress hostAddr = Inet4Address.getLocalHost();
		    host = hostAddr.getHostAddress();
	    } catch (UnknownHostException e) {
		e.printStackTrace();
	    }
            return host;
        }

       	private void notifyMethodCall(String interfaceName, String methodName) {
	    String chName = this.getChannelName(interfaceName, methodName);
	      		
	    String s = this.getHost()
		       + "|" + ContainerServant.getInstance().getName()
		       + "|"+ interfaceName
		       + "|"+ methodName;

            String logMessage = "statsCollection: methodNotification";

	    Any eventAny = ORB.init().create_any();
	    eventAny.insert_string(s);
            notifyEvent(chName,eventAny,logMessage);
        }

        private void notifyContainerStats(){
              String chName = this.getChannelName();
	      String logMessage = "statsCollection: containerNotification";

              Any eventAny = ORB.init().create_any();
	      ContainerStatsHelper.insert(eventAny, containerStats);

              notifyEvent(chName,eventAny,logMessage);

        }

        private void notifyEvent(String channelName, Any event, String logMessage){
            		
	    LogSender ls = new LogSender(logMessage,"localhost",514);

	    if( this.chManagement == null ) 
		return;
	
            this.chManagement.notifyChannel(channelName, event);
	}

        private boolean subscribeEvent(String clientName, String channelName, EventSink sink){
            boolean ret = false;

	    if( this.chManagement == null ) {
	        this.eventMgr = ContainerServant.getInstance().getEventMgr();

                if (this.eventMgr == null){
			System.err.println("StatsCollectionServant: Erro ao retornar eventMgr");
                        return ret;
		}	
	        
		this.chManagement = ChannelManagementHelper.narrow(this.eventMgr.getFacet("scs::event_service::ChannelManagement"));
		
	        if( this.chManagement== null ) {
	              System.err.println("StatsCollectionServant: Erro ao retornar ChannelManagement !");
		      return ret;
		}
	     }
		
	     return this.chManagement.subscribeChannel(clientName, channelName, sink);
	} 
        
        private void cancelEvent(String clientName , String channelName){
              if( this.chManagement== null ) 
                    return;
              this.chManagement.cancelChannel(clientName, channelName);
        }

	public InterfaceStats[] getComponentsStats() throws ComponentStatsNotAvailable {

                if (componentsStats.size() == 0)
                    throw new ComponentStatsNotAvailable ();

		return componentsStats.toArray(new InterfaceStats[componentsStats.size()]);
	}

	public MethodStats[] getInterfaceStats(String interfaceName) throws InterfaceStatsNotAvailable {
	
		for (Iterator iter = componentsStats.iterator(); iter.hasNext();) {
			InterfaceStats element = (InterfaceStats) iter.next();
			if(element.interfaceName.equalsIgnoreCase(interfaceName))
			{
				return	element.methodStatsCollection;
			}
		}
		throw new InterfaceStatsNotAvailable ();
	}

        public SystemIOStats getIOStats() throws IOStatsNotAvailable{
              IOStats iostats = SystemInformation.getIOUsage();

              if (iostats == null)
                   throw new IOStatsNotAvailable ();

              systemIOStats.sectorsRead = iostats.sectors_read;  
              systemIOStats.timeReading = iostats.time_spent_in_readings;
              systemIOStats.sectorsWriten = iostats.sectors_writen;
              systemIOStats.timeWriting = iostats.time_spent_in_writings; 
              systemIOStats.nfsSectorsRead = iostats.sectors_read_in_nfs; 
              systemIOStats.nfsSectorsWriten = iostats.sectors_writen_in_nfs;
      
              return systemIOStats;
        }

        public SystemNetworkStats getNetworkStats() throws NetworkStatsNotAvailable{
             NetworkStats netstats = SystemInformation.getNetworkUsage();

             if (netstats == null)
                 throw new NetworkStatsNotAvailable ();

             systemNetworkStats.bytesReceived = netstats.bytes_received;
             systemNetworkStats.bytesTransmitted = netstats.bytes_transmitted;

             return systemNetworkStats;
       }
	 
	public void setContainerName( String name ) {
		this.containerStats.containerName = name;
	}
	
	public boolean insertStatsCalls(String interfaceName , String methodName)
	{
		//System.out.println ("Dentro de StatsCall: " + interfaceName + " " + methodName);
		MethodStats ms = find(interfaceName , methodName);
		ms.callsCount++;
		
		notifyMethodCall( interfaceName, methodName );
		
		return true;
	}
	
	public boolean insertStatsCPU(String interfaceName , String methodName, long cpuUsage)
	{
		MethodStats ms = find(interfaceName , methodName);
		ms.cpuTime += cpuUsage;
		return true;
	}

	public boolean insertStatsElapsedTime(String interfaceName , String methodName, long elapsedTime)
	{
		MethodStats ms = find(interfaceName , methodName);
		ms.elapsedTime += elapsedTime;
		return true;
	}

	public boolean setContainerStatsCPUTime(long cpuTime)
	{
		this.containerStats.cpuTime = cpuTime;
		return true;
	}

	public boolean setContainerStatsCPUUsage(double cpuUsage)
	{
		this.containerStats.cpuUsage = cpuUsage;
		return true;
	}

	public void setContainerStatsAvgCPUUsage(double cpuUsage) {
		this.containerStats.avgCpuUsage = cpuUsage;
	}
	
	public boolean setContainerStatsElapsedTime(long elapsedTime)
	{
		this.containerStats.elapsedTime = elapsedTime;
		return true;
	}
	
	public void setContainerStatsMemoryUsage(long memoryUsage) {
		this.containerStats.memoryUsage = memoryUsage;
                notifyContainerStats();
	}

	public ContainerStats getContainerStats() {
		return this.containerStats;
	}
        
        
	public boolean subscribeMethodNotification(String clientName, String ifname, String method, EventSink sink) {
             String chName = this.getChannelName(ifname, method);
             return this.subscribeEvent(clientName, chName, sink);
        }
   
        public boolean subscribeContainerNotification(String clientName, EventSink sink) {
             String chName = this.getChannelName();
             return this.subscribeEvent(clientName, chName, sink);
        }
       
       	 public void cancelMethodNotification(String clientName,String ifname, String method) {
	      String chName = this.getChannelName(ifname, method);
	      this.cancelEvent(clientName, chName);
	 }

         public void cancelContainerNotification(String clientName) {
              String chName = this.getChannelName();
	      this.cancelEvent(clientName, chName);
         }

}
