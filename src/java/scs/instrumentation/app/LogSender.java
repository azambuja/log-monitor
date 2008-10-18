package scs.instrumentation.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Classe que facilita o envio de logs para o LogCollector
 */
public class LogSender {

		DatagramSocket socket=null;
		String host=null;
		int port;
		String process = null;
		
		public LogSender ( String process, String host, int port ) {

			try {
				socket = new DatagramSocket();
			} catch (SocketException e) {
				e.printStackTrace();
			} 	

			this.host = host;
			this.port = port;
			this.process = process;
		}
		
		public boolean sendEvent( String event ) {
				DatagramPacket packet=null;
				String msg = "[" + process + "] " + event;
				byte[] buffer = msg.getBytes();
				
				try {
					packet = new DatagramPacket(buffer,buffer.length,InetAddress.getByName(this.host),this.port);
					socket.send( packet );
					return true;
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
		}

}
