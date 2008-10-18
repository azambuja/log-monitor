package scs.instrumentation.interceptor;

import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

/**
 * Portable Interceptor que coleta as informacoes das chamadas 
 * dos componentes do processo.
 */
public class StatsServicePureInterceptor  extends org.omg.CORBA.LocalObject
implements ClientRequestInterceptor, ServerRequestInterceptor
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StatsServicePureInterceptor(Current piCurrent, int outCallIndicatorSlotId)
	{  

	}

	public String name()
	{
		return "StatsServicePureInterceptor";
	}
	
	public void destroy()
	{
	}
	
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

	public void receive_request_service_contexts(ServerRequestInfo ri)
	{
	}

	public void receive_request(ServerRequestInfo ri)
	{
	}
	
	public void send_reply(ServerRequestInfo ri)
	{
	}
	
	public void send_exception(ServerRequestInfo ri)
	{
	}
	
	public void send_other(ServerRequestInfo ri)
	{
	}
}

