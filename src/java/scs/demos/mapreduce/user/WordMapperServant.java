package scs.demos.mapreduce.user;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnyHolder;

import scs.demos.mapreduce.Mapper;
import scs.demos.mapreduce.MapperPOA;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.OutputCollector;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.schedule.LogError;


/**
 * Servant que implementa a interface scs::demos::mapreduce::Mapper
 * @author Sand Luz Correa
 */

public class WordMapperServant extends MapperPOA {
  
	public void map(Any key, Any value, OutputCollector collector, Reporter reporter) throws IOMapReduceException {
		try {
        	        String s1 = value.extract_string();
                        key.insert_string(s1);
			value.insert_string("1");
			collector.collect(key, value);
		} catch (IOMapReduceException e) {
			throw e;
		} catch (Exception e) {
			String exception = LogError.getStackTrace(e);
                        reporter.report(0,"WordMapperServant::map - " + exception);
                        throw new IOMapReduceException();
		}
	}
}
