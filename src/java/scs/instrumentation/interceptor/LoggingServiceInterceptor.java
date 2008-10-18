package scs.instrumentation.interceptor;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ClientRequestInfo;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.InvalidSlot;
import org.omg.PortableInterceptor.RequestInfo;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import scs.instrumentation.app.LogSender;

/**
 * Portable Interceptor que envia as informacoes das chamadas para o arquivo
 * de log e para o LogCollector.
 */
public class LoggingServiceInterceptor extends org.omg.CORBA.LocalObject
		implements ClientRequestInterceptor, ServerRequestInterceptor {


	/**
	 * Inner class que e uma thread de execucao que le os eventos de uma fila
	 * criada pelo interceptor para armazenar os eventos relacionados as chamadas
	 * dos objetos.
	 */
	public class LogSenderThread extends Thread  {
		BlockingQueue<String> eventQueue = null;

		LogSender sender = null;
		
		public LogSenderThread(String process, String host, BlockingQueue<String> queue) {
			this.eventQueue = queue;
			this.sender = new LogSender(process, host, 514);
		}
		
		@Override
		public void run() {
			
			while(true){
				
				String msg;
				
				try {
					msg = this.eventQueue.take();
				} catch (InterruptedException e) {
					System.err.println("LogSender interrupted ! Exiting ...");
					break;
				}

				this.sender.sendEvent(msg);
			}
		}
	}
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Current piCurrent;

	private int outCallIndicatorSlotId;

	private Logger logger = Logger.getLogger("LoggingServiceInterceptor");

	private LogSenderThread logsender=null;
	
	private BlockingQueue<String> queue = null;
	
	/**
	 * Nome do arquivo de log a ser criado
	 */
	private String logName = null;
	
	/**
	 * host que recebe os logs via syslog (LogCollector)
	 */
	private String logHost = null;
	
	public LoggingServiceInterceptor(Current piCurrent,
			int outCallIndicatorSlotId) {
		this.piCurrent = piCurrent;
		this.outCallIndicatorSlotId = outCallIndicatorSlotId;

		SimpleFormatter formatter = new SimpleFormatter();
		FileHandler fileHandler = null;

		try {
			Properties props = System.getProperties();
			
			String dir = props.getProperty("logInterceptor.dir", ".");

			logName = props.getProperty("logInterceptor.name");

			logHost = props.getProperty("logInterceptor.host", "localhost");

			if (logName == null) {
				Date d = new Date();
				logName = "logInterceptor_" + d.getTime();
			}

			String fname = dir + "/" + logName	+ ".log";
			
			fileHandler = new FileHandler(fname);
			fileHandler.setFormatter(formatter);
		} catch (Exception e) {
			System.out.println("*** Error opening log file: ***");
			e.printStackTrace();
		}

		this.logger.addHandler(fileHandler);

		this.queue = new LinkedBlockingQueue<String>(); 
		this.logsender = new LogSenderThread(logName, logHost, queue);
		this.logsender.start();
	}

	public String name() {
		return "LoggingServiceInterceptor";
	}

	public void destroy() {
	}

	//
	// ClientRequestInterceptor operations
	//
	public void send_request(ClientRequestInfo ri) {
		log(ri, "send_request");
	}

	public void send_poll(ClientRequestInfo ri) {
		log(ri, "send_poll");
	}

	public void receive_reply(ClientRequestInfo ri) {
		log(ri, "receive_reply");
	}

	public void receive_exception(ClientRequestInfo ri) {
		log(ri, "receive_exception");
	}

	public void receive_other(ClientRequestInfo ri) {
		log(ri, "receive_other");
	}

	// Server interceptor methods
	public void receive_request_service_contexts(ServerRequestInfo ri) {
		log(ri, "receive_request_service_contexts");
	}

	public void receive_request(ServerRequestInfo ri) {
		log(ri, "receive_request");
		logger.info("most derived interface: " + ri.target_most_derived_interface() );
	}

	public void send_reply(ServerRequestInfo ri) {
		log(ri, "send_reply");
	}

	public void send_exception(ServerRequestInfo ri) {
		log(ri, "send_exception");
	}

	public void send_other(ServerRequestInfo ri) {
		log(ri, "send_other");
	}

	//
	// Utilities
	//
	public void log(RequestInfo ri, String point) {
		// IMPORTANT: Always set the TSC outcall indicator in case
		// other interceptors make outcalls for this request.
		// Otherwise the outcall will not be set for the other interceptor's
		// outcall, resulting in infinite recursion.
		Any indicator = ORB.init().create_any();
		indicator.insert_boolean(true);
		try {
			piCurrent.set_slot(outCallIndicatorSlotId, indicator);
		} catch (InvalidSlot e) {
		}
		try {
			indicator = ri.get_slot(outCallIndicatorSlotId);
			// If the RSC outcall slot is not, set then log this invocation.
			// If it is, set that indicates the interceptor is servicing the
			// invocation of loggingService itself. In that case, do
			// nothing (to avoid infinite recursion).

			// if (indicator.type().kind().equals(TCKind.tk_null)) {
			String msg = point + "::" + ri.request_id() + "::" + ri.operation();
			logger.info(msg);
			this.queue.add(msg);
			// }

		} catch (InvalidSlot e) {
			System.out.println("Exception handling not shown.");
		}
	}
}
