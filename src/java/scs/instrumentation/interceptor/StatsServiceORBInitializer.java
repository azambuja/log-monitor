package scs.instrumentation.interceptor;

import org.omg.PortableInterceptor.*;

/**
 * Initializer do interceptor de estatisticas do SCS
 */
public class StatsServiceORBInitializer extends org.omg.CORBA.LocalObject
		implements org.omg.PortableInterceptor.ORBInitializer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StatsServiceORBInitializer() {
	}

	public void pre_init(ORBInitInfo info) {
	}

	public void post_init(ORBInitInfo info) {
		try {
			Current piCurrent = CurrentHelper.narrow(info
					.resolve_initial_references("PICurrent"));

			int outCallIndicatorSlotId = info.allocate_slot_id();

			StatsServiceInterceptor interceptor = new StatsServiceInterceptor(
					piCurrent, outCallIndicatorSlotId);
			info.add_client_request_interceptor(interceptor);
			info.add_server_request_interceptor(interceptor);
			System.err.println("StatsServiceInterceptor installed");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}