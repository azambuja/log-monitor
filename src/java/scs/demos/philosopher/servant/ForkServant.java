package scs.demos.philosopher.servant;

import scs.demos.philosopher.ForkPOA;

public class ForkServant extends ForkPOA {

	boolean inUse = false;
	
	public boolean get() {
		boolean ret = false;
		
		synchronized(this) {
			if( !inUse ) {
				inUse = true;
				ret = true;
			}
			
			return ret;
		}
	}
	public boolean release() {
		boolean ret = false;

		synchronized( this ) {
			if( inUse ) {
				inUse = false;
				ret = true;
			}
		}
		
		return ret;
	}

}
