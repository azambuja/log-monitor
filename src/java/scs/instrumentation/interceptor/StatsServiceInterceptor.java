package scs.instrumentation.interceptor;

import java.util.Hashtable;
import java.util.ArrayList;

import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import scs.instrumentation.app.CallData;
import scs.instrumentation.servant.StatsCollectionServant;
import system.SystemInformation;
import system.SystemInformation.NegativeCPUTime;


/**
 * Portable Interceptor que coleta as informacoes das chamadas 
 * dos componentes do processo.
 */
public class StatsServiceInterceptor  extends org.omg.CORBA.LocalObject
implements ClientRequestInterceptor, ServerRequestInterceptor
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Hashtable<Integer,CallData> reqTime  = new Hashtable<Integer,CallData>();
	private ArrayList<String> interceptedMethods = new ArrayList<String>(); 	
	

	public StatsServiceInterceptor(Current piCurrent, int outCallIndicatorSlotId)
	{  
		interceptedMethods.add("ping");
		interceptedMethods.add("execute");
	        interceptedMethods.add("map");
		interceptedMethods.add("reduce");
		interceptedMethods.add("start");
                interceptedMethods.add("pong");
	}

	public String name()
	{
		return "StatsServiceInterceptor";
	}
	
	public void destroy()
	{
	}
	
	//
	// ClientRequestInterceptor operations
	//
	public void send_request(ClientRequestInfo ri)
	{

	}
	
	public void send_poll(ClientRequestInfo ri)
	{
	}
	public void receive_reply(ClientRequestInfo ri)
	{
	}
	
	public void receive_exception(ClientRequestInfo ri)
	{
	}
	
	public void receive_other(ClientRequestInfo ri)
	{
	}

	// Server interceptor methods
	public void receive_request_service_contexts(ServerRequestInfo ri)
	{
	}

	public void receive_request(ServerRequestInfo ri)
	{
		if(!interceptedMethods.contains(ri.operation()))
			return;

                SystemInformation.CPUUsageSnapshot start = null;
	        	
		try {
			start = SystemInformation.makeCPUUsageSnapshot();
		} catch (NegativeCPUTime e) {
			e.printStackTrace();
		}

		CallData cd = new CallData(start, ri.target_most_derived_interface());
		
		reqTime.put(ri.request_id(), cd);
	}
	
	public void send_reply(ServerRequestInfo ri)
	{
		if(!interceptedMethods.contains(ri.operation()))
			return;

		SystemInformation.CPUUsageSnapshot end = null;
		try {
			end = SystemInformation.makeCPUUsageSnapshot();
		} catch (NegativeCPUTime e) {
			e.printStackTrace();
		}
		CallData stats = reqTime.get(ri.request_id());
		StatsCollectionServant statsCollection = StatsCollectionServant.getInstance();
		
		statsCollection.insertStatsCalls(stats.getInterfaceName(),ri.operation());

		statsCollection.insertStatsCPU(stats.getInterfaceName(),ri.operation(),(end.m_CPUTime - stats.getStart().m_CPUTime));
		statsCollection.insertStatsElapsedTime(stats.getInterfaceName(),ri.operation(), end.m_time - stats.getStart().m_time);
		
		reqTime.remove(ri.request_id());
	}
	
	public void send_exception(ServerRequestInfo ri)
	{
	}
	
	public void send_other(ServerRequestInfo ri)
	{
	}
}

