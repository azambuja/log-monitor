package scs.demos.mapreduce.servant;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnyHolder;

import scs.demos.mapreduce.Partitioner;
import scs.demos.mapreduce.PartitionerPOA;


/**
 * Servant que implementa a interface scs::demos::mapreduce::Partitioner
 * @author Sand Luz Correa
 */

public class PartitionerServant extends PartitionerPOA {
  
	public int getPartition (Any key, Any value, int numPartitions) {
		String s = key.extract_string();
		return Math.abs(s.hashCode()) % numPartitions;
	}

	public Comparator getPartitionComparator() {
          	return new Comparator() { 
       		 	public int compare(Object obj1, Object obj2) {
                      		Any key1 =(Any) obj1;
				Any key2 =(Any) obj2;
				return key1.extract_string().compareTo(key2.extract_string());
			}
		};
	}

}


