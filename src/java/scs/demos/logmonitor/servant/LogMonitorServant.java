package scs.demos.logmonitor.servant;

import scs.demos.logmonitor.LogMonitor;
import scs.demos.logmonitor.LogMonitorHelper;
import scs.demos.logmonitor.LogMonitorPOA;

import scs.core.ConnectionDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidName;

public class LogMonitorServant extends LogMonitorPOA {
     
        private int identifier;
        private LogMonitorComponent logmonitor = null;
        private IReceptacles infoReceptacle = null;
        private ConnectionDescription conns[];
        private int logMonitorCount; 
        
        public  LogMonitorServant(LogMonitorComponent logmonitor){
           this.logmonitor = logmonitor;
        }
        
        public void setId(int identifier){
             this.identifier = identifier;
        }   

        public int getId(){
           return identifier;
        }

        public void start() {
        	logMonitorCount = 10;
        	infoReceptacle = IReceptaclesHelper.narrow(logmonitor.getFacetByName("infoReceptacle"));

			try {
				conns = infoReceptacle.getConnections("LogMonitor");
			} catch (InvalidName e) {
				e.printStackTrace();
			}

			System.out.println("LogMonitor " + identifier + " received an start call!");
   
			if (identifier==1)    /* Chama metodo ping do componente conectado*/
			{
				for (int i = 0; i < conns.length; i++) {
					LogMonitor logFacet = LogMonitorHelper.narrow( conns[i].objref );
					logFacet.ping();
				}
			}
        }

        public void stop() {
        	logMonitorCount = 0;
        }

       public void ping() {
		for (int i = 0; i < conns.length; i++) {
               		LogMonitor logFacet = LogMonitorHelper.narrow( conns[i].objref );
               		System.out.println("Received ping from " + logFacet.getId() );
               		//try {
			//	Thread.sleep(1000);
              		//} catch (InterruptedException e) {
            	   	//	e.printStackTrace();
               		//}
                        logFacet.pong();
                }
       }
                  
	
        public void pong() {
		for (int i = 0; i < conns.length; i++) {
               		LogMonitor logFacet = LogMonitorHelper.narrow( conns[i].objref );
               		System.out.println("Received pong from " + logFacet.getId() );
               		if( --this.logMonitorCount > 0 ) {
            	   		//try {
            			//	   Thread.sleep(1000);
            	   		//} catch (InterruptedException e) {
            			//  	   e.printStackTrace();
            	  		//}
            	  		logFacet.ping();
               		}
            	}
	
	} 
}

