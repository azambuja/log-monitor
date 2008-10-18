package scs.demos.mapreduce.servant;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.CORBA.AnyHolder;

import scs.demos.mapreduce.IOFormat;
import scs.demos.mapreduce.TaskStatus;
import scs.demos.mapreduce.OutputCollector;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.Mapper;
import scs.demos.mapreduce.RecordReader;
import scs.demos.mapreduce.IOFormatHelper;
import scs.demos.mapreduce.MapperHelper;
import scs.demos.mapreduce.OutputCollectorHelper;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.schedule.LogError;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.FileSplitHelper;
import scs.demos.mapreduce.IOFormatException;
import scs.demos.mapreduce.Reducer;
import scs.demos.mapreduce.Partitioner;
import scs.demos.mapreduce.ReducerHelper;

/**
 * Especializa a classe TaskServant para criar uma 
 * tarefa map 
 * @author Sand Luz Correa
*/

public class MapTaskServant extends TaskServant {

        private IOFormat ioformat = null;
	private RecordReader input = null;

        private int numReducers = 0;
        private OutputCollector collector = null;
	private Mapper mapper = null;
        private PartitionerServant partitioner = null;
        private Reducer combiner = null;
        private String exception = null;
        private POA poa = null;

        private final String SUFIX = ".txt";

        private IOFormat createIOFormat() {
		IOFormat ioformat = null;
                String ioformatClassName = conf.getProperty("mapred.IOFormat.class-name");
        	
		try{	
	       		Servant obj = (Servant) Class.forName(ioformatClassName).newInstance();
	       		ioformat = IOFormatHelper.narrow(poa.servant_to_reference(obj));
	     	} catch (Exception e){
	     		exception = LogError.getStackTrace(e);
	     	} 
	     	return ioformat;
        }

        private OutputCollector createCollector() {
		OutputCollector collector = null;
		try {
                        /* Obtem o nome do arquivo de entrada*/
                	String inputFile = conf.getProperty("mapred.Input.name");                
                	String[] split = inputFile.split(SUFIX);
                                               		
			if (numReducers > 0) {
                        	outputSplit = new FileSplit[numReducers];
		            	for (int i = 0; i<outputSplit.length; i++) {
					outputSplit[i] = FileSplitHelper.narrow(poa.servant_to_reference(
							 new FileSplitServant(split[0] + id + "mapped" + i + SUFIX)));
                                }
                                collector = OutputCollectorHelper.narrow(poa.servant_to_reference(
					    new BufferOutputCollectorServant(this))); 
			}
			else { 
				outputSplit = new FileSplit[1]; 
                                outputSplit[0] = FileSplitHelper.narrow(poa.servant_to_reference(
					         new FileSplitServant(split[0] + id + "mapped" + SUFIX))); 
 				collector = OutputCollectorHelper.narrow(poa.servant_to_reference(
					    new DirectOutputCollectorServant(ioformat,reporter,outputSplit[0],configFileName,status)));
                        }	
		} catch (Exception e) {
			exception = LogError.getStackTrace(e);
                }
                return collector;
	}

	private Mapper createMapper() {
		Mapper mapper = null;
                String mapperClassName = conf.getProperty("mapred.Mapper.servant-name");

	     	try{	
	       		Servant obj = (Servant) Class.forName(mapperClassName).newInstance();
	            	mapper = MapperHelper.narrow(poa.servant_to_reference(obj));
	     	} catch (Exception e){
                        exception = LogError.getStackTrace(e);
            	} 
	     	return mapper;
	}

	private PartitionerServant createPartitioner() {
                String partitionerClassName = conf.getProperty("mapred.Partitioner.servant-name");
		PartitionerServant partitioner = null;
 
	     	try{	
	       		partitioner = (PartitionerServant) Class.forName(partitionerClassName).newInstance();
	     	} catch (Exception e){
                        exception = LogError.getStackTrace(e);
            	} 
	     	return partitioner;
	}

	private Reducer createCombiner() {
                Reducer combiner = null;
                String reducer = conf.getProperty("mapred.Reducer.servant-name");
                String combinerClassName = conf.getProperty("mapred.Combiner.servant-name",reducer);

	     	try{	
	       		Servant obj = (Servant) Class.forName(combinerClassName).newInstance();
	            	combiner = ReducerHelper.narrow(poa.servant_to_reference(obj));
	     	} catch (Exception e){
                        exception = LogError.getStackTrace(e);
            	} 
	     	return combiner;
	}

        public MapTaskServant(String configFileName, Reporter reporter) throws IOException {
		super(configFileName, reporter);
		status = TaskStatus.END;
	}

        public MapTaskServant(String configFileName, Reporter reporter,	POA poa, FileSplit inputSplit) throws IOException {
		super(configFileName, reporter);
                this.poa = poa; 
                this.inputSplit = new FileSplit[1];
		this.inputSplit[0] = inputSplit;
 		status = TaskStatus.MAP;

                /*Obtem IOFormat*/
                ioformat = createIOFormat();
                if(ioformat == null) {
                        reporter.report(0, "MapTaskServant::MapTaskServant - Erro ao instanciar ioformat. \n" + exception);
			throw new IOException(); 
                }
        
                /*Obtem RecordReader*/
                try {
        		input = ioformat.getRecordReader(status);
                } catch (IOFormatException e) {
                        reporter.report(0,"MapTaskServant::MapTaskServant - Erro ao instanciar RecordReader. \n" + e.getMessage());
			throw new IOException();
		}
		
		/*Obtem numero de reducers*/
		numReducers = Integer.parseInt(conf.getProperty("mapred.Reducers.number","0"));

                /* Obtem mapper*/
                mapper = createMapper();
                if (mapper == null) {
                        reporter.report(0, "MapTaskServant::MapTaskServant - Erro ao instanciar mapper. " + exception);
			throw new IOException(); 
                }

                /* Obtem partitioner*/
                if (numReducers > 0 ) {
			partitioner = createPartitioner();	
                        if (partitioner == null) {
                                reporter.report(0,"MapTaskServant::MapTaskServant - Erro ao instanciar partition. " + exception);
				throw new IOException();
			}
                        boolean combineFlag = Boolean.valueOf(conf.getProperty("mapred.Combine.flag","false")).booleanValue();
                        if (combineFlag) {
				combiner = createCombiner();
				if (combiner == null) {
                                        reporter.report(0, "MapTaskServant::MapTaskServant - Erro ao instanciar combiner. " + exception);
					throw new IOException();
				}
			}	 
                }
              
                /* Obtem collector*/
                collector = createCollector();
               	if (collector == null) {
                        reporter.report(0,"MapTaskServant::MapTaskServant - Erro ao instanciar OutputCollector \n" + exception);
                	throw new IOException();
		}		
        }

	public void doRun() throws IOMapReduceException { 
                try {
                        input.open(configFileName,inputSplit[0],reporter);
                        AnyHolder key = new AnyHolder();
                        AnyHolder value = new AnyHolder();

                        while(input.next(key,value))
                        {
				mapper.map(key.value, value.value, collector, reporter);
                        }
			collector.flush();
                        collector.close();                        
			input.close();
                        collector = null;
			input = null;              
   
                } catch (IOMapReduceException e) {
			e.printStackTrace();
                        throw e;
		} catch (Exception e) {
			exception = LogError.getStackTrace(e);
                        reporter.report(0,"MapTask::doRun - " + exception);
                        throw new IOMapReduceException(exception);
		}
	}
        
        public IOFormat getIOFormat() {
		return ioformat;
	}
	
	public Reporter getReporter() {
		return reporter;
	}
	
	public String getConfigFileName() {
		return configFileName;
	}

	public PartitionerServant getPartitioner() {
		return partitioner;
	}
 
        public Reducer getCombiner() {
		return combiner;
	}	
        
        public int getNumReducers() {
		return numReducers;
	}

        public POA getPoa() {
		return poa;
	}
}         

