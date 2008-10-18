package scs.demos.mapreduce.servant;

import java.io.*;
import java.util.*;
import org.omg.CORBA.Any;
import org.omg.CORBA.AnyHolder;
import org.omg.CORBA.ORB;

import scs.demos.mapreduce.OutputCollectorPOA;
import scs.demos.mapreduce.schedule.LogError;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.RecordWriter;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.IOFormat;
import scs.demos.mapreduce.Partitioner;
import scs.demos.mapreduce.Reducer;
import scs.demos.mapreduce.OutputCollector;
import scs.demos.mapreduce.OutputCollectorHelper;
import org.omg.PortableServer.POA;
import scs.demos.mapreduce.TaskStatus;

/**
 * Servant que implementa a interface OutputCollector usada
 * na operação map quanto o numero de reducers é maior que zero.
 */
public class BufferOutputCollectorServant extends OutputCollectorPOA {
       
        private Reporter reporter = null;
        private IOFormat ioformat = null;
        private FileSplit[] outputSplit = null;  
        private String exception = null;
        private MapTaskServant task = null;           
 	private PartitionerServant partitioner = null;
        private Reducer combiner = null;
        private RecordWriter[] out = null;
        private int numReducers = 0;
        private String configFileName = null;
        private POA poa = null;
                
	private class ObjToSort {
                public Any key;
                public Any value;

                public ObjToSort(Any key, Any value) {
                        this.key = key;
                        this.value = value;
                }

		public ObjToSort() { 
			key = null;
			value = null;
		}

        }

        private class SortComparator implements Comparator {
		Comparator comp = null;
		public SortComparator(Comparator comp) {
			this.comp = comp;
		}
                public int compare(Object obj1,Object obj2) {
                        ObjToSort o1 = (ObjToSort) obj1;
			ObjToSort o2 = (ObjToSort) obj2;
			return comp.compare((Object) o1.key,(Object) o2.key);
		}
	}
	
        
        private ArrayList<ObjToSort> [] segment = null;


        public BufferOutputCollectorServant(MapTaskServant task) throws IOMapReduceException {
        	try {
                        
			this.task =  task;
             		this.reporter = task.getReporter();
	     		this.ioformat = task.getIOFormat();
             		this.outputSplit = task.getOutput();
	     		this.partitioner = task.getPartitioner();
	     		this.combiner = task.getCombiner();    
	 		this.numReducers = task.getNumReducers();
	 		this.configFileName = task.getConfigFileName();
                        this.poa = task.getPoa();		
            
             		this.out = new RecordWriter[numReducers]; 
             		this.segment =  new ArrayList[numReducers];

             		for (int i= 0; i < segment.length; i++) {
				segment[i] = null;                  		             
             		}
		}catch (Exception e ) {
                        exception = LogError.getStackTrace(e);
                        reporter.report(0,"BufferOutputCollectorServant::BufferOutputCollectorServant - " + exception);
                        throw new IOMapReduceException ();
		}    
        }

        public void close() throws IOMapReduceException { 
                try {
			if (this.out != null) {
                                for (int i = 0; i < out.length;i++) {
                                        if (out[i] != null) {
        					out[i].close();
					}
				}
			}
      		} catch (Exception e) {
                        exception = LogError.getStackTrace(e);
                        reporter.report(0,"BufferOutputCollectorServant::close - " + exception);
			throw new IOMapReduceException();
		}

      	}

    	public void flush() throws IOMapReduceException {
                try {
 			/* Ordena cada particao e grava arquivo de saida*/
                	SortComparator comparator = new SortComparator (partitioner.getPartitionComparator());
                
			for(int i=0; i < outputSplit.length; i++) {
                		if (segment[i] == null ) {
                                	continue;
                    		}
                        
                    		Object [] objArray = segment[i].toArray();
                    		Arrays.sort(objArray, comparator);

                    		if (combiner == null) {
                                	out[i] = ioformat.getRecordWriter(TaskStatus.MAP);
		        		out[i].open(configFileName,outputSplit[i],reporter);
                                        
					for(int j=0; j < objArray.length; j++) {
                               			ObjToSort o = (ObjToSort) objArray[j];
                        			out[i].write(o.key, o.value);
                       			}
                                        objArray = null;
		           		out[i].close();                                         
				}
                    		else {  
					ObjToSort last = (ObjToSort) objArray[0];
                            		ObjToSort current = null; 
					ArrayList<Any> anyList = new ArrayList<Any> ();
                                	OutputCollector combineCollector = OutputCollectorHelper.narrow(poa.servant_to_reference(
					                           new DirectOutputCollectorServant(ioformat, reporter,
								   outputSplit[i], configFileName,TaskStatus.MAP)));

					for(int j=0; j < objArray.length; j++) {
                                		current = (ObjToSort) objArray[j];
                                   		if (comparator.compare((Object)last,(Object)current)==0) {
                                        		anyList.add(current.value);
                                            		last = current;
						}
                                    		else {
                                             		Any[] values = new Any[anyList.size()];
							anyList.toArray(values);
							combiner.reduce(last.key, values, combineCollector, reporter);
							values = null;
                                             		anyList.clear();
							anyList.add(current.value);
							last = current;
						}
					}
                              		if (anyList.size() > 0) { 
						Any[] values = new Any[anyList.size()];
						anyList.toArray(values);
						combiner.reduce(last.key, values, combineCollector, reporter);
						values = null;
                                    		anyList.clear();
                              		}
					objArray = null;
                              		combineCollector.flush();
					combineCollector.close();
                                	combineCollector = null;
		           	}
			   	//objArray = null;
		           	//out[i].close();
                	}
		} catch (Exception e) {
			exception = LogError.getStackTrace(e);
                        reporter.report(0,"BufferOutputCollectorServant::flush - " + exception);   
                        throw new IOMapReduceException ();
		}
    	}

    	public void collect(Any key, Any value) throws IOMapReduceException {
        	try{
			ObjToSort objToSort = new ObjToSort(key, value);
                        int index = partitioner.getPartition(objToSort.key,objToSort.value,numReducers);
                        if (segment[index] == null) {
	                        segment[index] = new ArrayList<ObjToSort> ();
                        }
                        segment[index].add(objToSort);
                } catch (Exception e) {
			exception = LogError.getStackTrace(e);
			reporter.report(0,"BufferOutputCollectorServant::collect - " + exception); 
                       throw new IOMapReduceException ();
		}
        }
}
