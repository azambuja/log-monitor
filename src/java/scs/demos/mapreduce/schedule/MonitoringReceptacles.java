package scs.demos.mapreduce.schedule;

import java.util.ArrayList;

import org.omg.CORBA.Object;

import scs.core.servant.IReceptaclesServant;
import scs.core.servant.Receptacle;

public class MonitoringReceptacles extends IReceptaclesServant {

        @Override
        protected ArrayList<Receptacle> createReceptacles() {
                ArrayList<Receptacle> receptacles = new ArrayList<Receptacle>();

                Receptacle rcpt = new Receptacle("Monitoring", "scs::reasoning::Monitor", true);
                receptacles.add(rcpt);
                return receptacles;
        }

        @Override
        protected int getConnectionLimit() {
                return 100;
        }

        @Override
        protected boolean isValidConnection(Object obj) {
                return true;
        }

}

