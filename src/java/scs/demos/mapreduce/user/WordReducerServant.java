package scs.demos.mapreduce.user;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnyHolder;

import scs.demos.mapreduce.Reducer;
import scs.demos.mapreduce.ReducerPOA;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.OutputCollector;
import scs.demos.mapreduce.schedule.LogError;


/**
 * Servant que implementa a interface scs::demos::mapreduce::Reducer
 * @author Sand Luz Correa
 */

public class WordReducerServant extends ReducerPOA {
  
	public void reduce(Any key, Any[] values, OutputCollector collector, Reporter reporter) throws IOMapReduceException {
            	try {
                        int count = 0;
                        String currentValue;
                	for(int i=0; i < values.length; i++){
                                currentValue = values[i].extract_string();
				count = count + Integer.parseInt(currentValue);
			}
                        Any value = values[0];
                        value.insert_string(String.valueOf(count));	
  			collector.collect(key,value);
		} catch (Exception e) {
                        String exception = LogError.getStackTrace(e);
                        reporter.report(0,"WordMapperServant::map - " + exception);
                        throw new IOMapReduceException(); 
		}
	}
}

