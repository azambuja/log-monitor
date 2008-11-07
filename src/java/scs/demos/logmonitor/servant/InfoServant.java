package scs.demos.logmonitor.servant;

import org.omg.CORBA.Object;
import scs.core.servant.IReceptaclesServant;
import scs.core.servant.Receptacle;
import java.util.ArrayList;

public class InfoServant extends IReceptaclesServant {
        protected ArrayList<Receptacle> createReceptacles() {
                ArrayList<Receptacle> receptacles = new ArrayList<Receptacle>();
                Receptacle rcpt = new Receptacle("LogMonitor", "scs::demos::logmonitor::LogMonitor", false);
                receptacles.add(rcpt);
                return receptacles;
        }

        protected int getConnectionLimit() {
                return 1;
        }

        protected boolean isValidConnection(Object obj) {
                return true;
        }
}

