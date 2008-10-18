package scs.demos.philosopher.servant;

import java.util.ArrayList;

import org.omg.CORBA.Object;

import scs.core.servant.IReceptaclesServant;
import scs.core.servant.Receptacle;

public class HandsServant extends IReceptaclesServant {

	private static final String RECEPTACLE_IFACE  = "scs::core::IReceptacles";
	private String receptacleName = "";
	
	public HandsServant( String name )
	{
		this.receptacleName = name;
	}
	
	@Override
	protected ArrayList<Receptacle> createReceptacles() {
		ArrayList<Receptacle> receptacles = new ArrayList<Receptacle>();
		
		Receptacle rcpt = new Receptacle(this.receptacleName, RECEPTACLE_IFACE, true);
		receptacles.add(rcpt);
		return receptacles;
	}

	@Override
	protected int getConnectionLimit() {
		return 1;
	}

	@Override
	protected boolean isValidConnection(Object obj) {
		//if( obj.getClass().getName().equals("Fork") )
		//	return true;
		return true;
	}

}
