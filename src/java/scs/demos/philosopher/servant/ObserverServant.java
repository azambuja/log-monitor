package scs.demos.philosopher.servant;

import scs.demos.philosopher.ObserverPOA;
import scs.demos.philosopher.PhilosopherState;
import scs.demos.philosopher.StatusInfo;

public class ObserverServant extends ObserverPOA {

	String getState( PhilosopherState st ) {
		
		if( st == PhilosopherState.EATING ) 	return "EATING"; 
		else if( st == PhilosopherState.THINKING ) return "THINKING";
		else if( st == PhilosopherState.HUNGRY ) return "HUNGRY";
		else if( st == PhilosopherState.STARVING) return "STARVING";
		else if( st == PhilosopherState.DEAD ) return "DEAD";

		return "????";
	}
	
	public void push(StatusInfo info) {
		
		synchronized(this) {
			
			System.out.print( info.name + "\t" + getState(info.state) + "\t" );
			if( info.has_left_fork )
				System.out.print(" L ");
			else 
				System.out.print(" - ");
	
			if( info.has_right_fork )
				System.out.print(" R ");
			else 
				System.out.print(" - ");
			
			System.out.print( "ticks: " + info.ticks_since_last_meal );
			
			//for( int i = 1; i <= info.ticks_since_last_meal ; i++ ) 
			//	System.out.print(".");
	
			System.out.println("");
		}
	}

}
