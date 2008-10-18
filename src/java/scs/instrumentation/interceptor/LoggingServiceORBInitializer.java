package scs.instrumentation.interceptor;

import java.util.logging.Logger;

import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.CurrentHelper;
import org.omg.PortableInterceptor.ORBInitInfo;

/**
 * Classe que implementa o initializer para configurar o LogInterceptor
 */
public class LoggingServiceORBInitializer extends org.omg.CORBA.LocalObject
		implements org.omg.PortableInterceptor.ORBInitializer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public LoggingServiceORBInitializer() {
		Logger logger = Logger.getLogger("LoggingServiceORBInitializer");
		logger.info("LoggingServiceORBInitializer::Constructor called");
	}

	public void pre_init(ORBInitInfo info) {
		Logger logger = Logger.getLogger("LoggingServiceORBInitializer");
		logger.info("LoggingServiceORBInitializer::pre_init called");
		
	}

	public void post_init(ORBInitInfo info) {
		Logger logger = Logger.getLogger("LoggingServiceORBInitializer");
		logger.info("LoggingServiceORBInitializer::post_init called");
		try {
			Current piCurrent = CurrentHelper.narrow(info
					.resolve_initial_references("PICurrent"));

			int outCallIndicatorSlotId = info.allocate_slot_id();

			LoggingServiceInterceptor interceptor = new LoggingServiceInterceptor(
					piCurrent, outCallIndicatorSlotId);
			info.add_client_request_interceptor(interceptor);
			info.add_server_request_interceptor(interceptor);
			System.err.println("LoggingServiceInterceptor installed");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}