package scs.demos.pingpong.servant;

import scs.demos.pingpong.PingPong;
import scs.demos.pingpong.PingPongHelper;
import scs.demos.pingpong.PingPongPOA;

import scs.core.ConnectionDescription;
import scs.core.IReceptacles;
import scs.core.IReceptaclesHelper;
import scs.core.InvalidName;

public class PingPongServant extends PingPongPOA {
     
        private int identifier;
        private PingPongComponent pingpong = null;
        private IReceptacles infoReceptacle = null;
        private ConnectionDescription conns[];
        private int pingPongCount; 
        
        public  PingPongServant(PingPongComponent pingpong){
           this.pingpong = pingpong;
        }
        
        public void setId(int identifier){
             this.identifier = identifier;
        }   

        public int getId(){
           return identifier;
        }

        public void start() {

        	pingPongCount = 10;
        	
        	infoReceptacle = IReceptaclesHelper.narrow(pingpong.getFacetByName("infoReceptacle"));

			try {
				conns = infoReceptacle.getConnections("PingPong");
			} catch (InvalidName e) {
				e.printStackTrace();
			}

			System.out.println("PingPong " + identifier + " received an start call!");
   
			if (identifier==1)    /* Chama metodo ping do componente conectado*/
			{
				for (int i = 0; i < conns.length; i++) {
					PingPong ppFacet = PingPongHelper.narrow( conns[i].objref );
					ppFacet.ping();
				}
			}
        }

        public void stop() {
        	pingPongCount = 0;
        }

       public void ping() {
		for (int i = 0; i < conns.length; i++) {
               		PingPong ppFacet = PingPongHelper.narrow( conns[i].objref );
               		System.out.println("Received ping from " + ppFacet.getId() );
               		//try {
			//	Thread.sleep(1000);
              		//} catch (InterruptedException e) {
            	   	//	e.printStackTrace();
               		//}
                        ppFacet.pong();
                }
       }
                  
	
        public void pong() {
		for (int i = 0; i < conns.length; i++) {
               		PingPong ppFacet = PingPongHelper.narrow( conns[i].objref );
               		System.out.println("Received pong from " + ppFacet.getId() );
               		if( -- this.pingPongCount > 0 ) {
            	   		//try {
            			//	   Thread.sleep(1000);
            	   		//} catch (InterruptedException e) {
            			//  	   e.printStackTrace();
            	  		//}
            	  		ppFacet.ping();
               		}
            	}
	
	} 
}

