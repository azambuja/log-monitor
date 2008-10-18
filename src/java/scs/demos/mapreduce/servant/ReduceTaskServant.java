package scs.demos.mapreduce.servant;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.CORBA.AnyHolder;
import org.omg.CORBA.Any;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import scs.demos.mapreduce.IOFormat;
import scs.demos.mapreduce.TaskStatus;
import scs.demos.mapreduce.OutputCollector;
import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.Mapper;
import scs.demos.mapreduce.RecordReader;
import scs.demos.mapreduce.RecordWriter;
import scs.demos.mapreduce.IOFormatHelper;
import scs.demos.mapreduce.MapperHelper;
import scs.demos.mapreduce.OutputCollectorHelper;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.schedule.LogError;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.FileSplitHelper;
import scs.demos.mapreduce.IOFormatException;
import scs.demos.mapreduce.Reducer;
import scs.demos.mapreduce.ReducerHelper;

/**
 * Especializa a classe TaskServant para criar uma 
 * tarefa map 
 * @author Sand Luz Correa
*/

public class ReduceTaskServant extends TaskServant {

        private IOFormat ioformat = null;
	private ArrayList<FileSplit> arraySplit = null;
        private OutputCollector collector = null;
	private String exception = null;
        private final String SUFIX = ".txt";
        private String[] path = null; 
        private POA poa;
        private Reducer reducer = null;
        private PartitionerServant partitioner = null;
        private int index;

	private class Item {
		public Any key;
                public Any value;
		public int index;
                
		public Item(Any key, Any value, int index) {
                        this.key = key;
                        this.value = value;
                        this.index = index;
                }
	}

        private class MergeComparator implements Comparator{
                Comparator comp = null;
		public MergeComparator(Comparator comp) {
			this.comp = comp;
		}
                public int compare(Object obj1,Object obj2) {
                        Item o1 = (Item) obj1;
			Item o2 = (Item) obj2;
			return comp.compare((Object) o1.key,(Object) o2.key);
		}
	} 
        
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
                        outputSplit = new FileSplit[1];                        		
			outputSplit[0] = FileSplitHelper.narrow(poa.servant_to_reference(
				      new FileSplitServant(path[0] + id + "reduced" + index + SUFIX))); 
 			collector = OutputCollectorHelper.narrow(poa.servant_to_reference(
				      new DirectOutputCollectorServant(ioformat,reporter,outputSplit[0],configFileName, status)));
		} catch (Exception e) {
			exception = LogError.getStackTrace(e);
                }
                return collector;
	}

	private Reducer createReducer() {
                Reducer reducer = null;
                String reducerName = conf.getProperty("mapred.Reducer.servant-name");
                
	     	try{	
	       		Servant obj = (Servant) Class.forName(reducerName).newInstance();
	            	reducer = ReducerHelper.narrow(poa.servant_to_reference(obj));
	     	} catch (Exception e){
                        exception = LogError.getStackTrace(e);
            	} 
	     	return reducer;
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

         
        private void merge(RecordReader[] r, RecordWriter w) throws IOMapReduceException {
                try {
                	AnyHolder key = new AnyHolder();
                	AnyHolder value = new AnyHolder();
			PriorityQueue queue = new PriorityQueue (r.length,new MergeComparator(partitioner.getPartitionComparator()));
                	Item item = null;

                	for(int i=0; i<r.length;i++) {
				if(r[i].next(key,value)) {
					item = new Item (key.value,value.value, i);
                                	queue.add(item); 
				}  
			}
                	item = null;
                	while (queue.size() != 0) {
				item = (Item) queue.poll();
				w.write(item.key, item.value);
                        	int minInd = item.index;
				item = null;

				if(r[minInd].next(key,value)) {
                              		item = new Item (key.value,value.value, minInd);
                              		queue.add(item); 
				}
			}                         
		}catch (Exception e) {
                        exception = LogError.getStackTrace(e);
			reporter.report(0,"ReduceTaskServant::merge - Erro ao fazer merge de reducer " 
			+ this.index + "\n" + exception);
                        throw new IOMapReduceException ();
		} 
	}

        public ReduceTaskServant(String configFileName, Reporter reporter, POA poa, ArrayList<FileSplit> arraySplit, int index) 
                                throws IOException {
		super(configFileName, reporter);
                this.poa = poa; 
		this.index = index;
                this.arraySplit = arraySplit;
                this.inputSplit = new FileSplit[this.arraySplit.size()];
                this.arraySplit.toArray(inputSplit);
       		status = TaskStatus.REDUCE;

                /* Obtem o nome do arquivo de entrada*/
                String inputFile = conf.getProperty("mapred.Input.name");                
                path = inputFile.split(SUFIX);

                /*Obtem IOFormat*/
                ioformat = createIOFormat();
                if(ioformat == null) {
                        reporter.report(0,"ReduceTaskServant::ReduceTaskServant - Erro ao instanciar ioformat. \n" + exception);  
			throw new IOException(); 
                }
                       		
		/* Obtem reducer*/
                reducer = createReducer();
                if (reducer == null) {
                        reporter.report(0,"ReduceTaskServant::ReduceTaskServant - Erro ao instanciar mapper.\n" + exception);
			throw new IOException(); 
                }
                
                /* Obtem partitioner*/
                partitioner = createPartitioner();
                if (partitioner == null) {
                        reporter.report(0,"ReduceTaskServant::ReduceTaskServant - Erro ao instanciar partitioner.\n" + exception);
			throw new IOException();
                } 

                /* Obtem collector para o reduce*/
		collector = createCollector();
               	if (collector == null) {
                        reporter.report(0,"ReduceTaskServant::ReduceTaskServant - Erro ao instanciar OutputCollector \n" + exception); 
                	throw new IOException();
		}
        }

	public void doRun() throws IOMapReduceException { 
                try {
                        int k = 4;
                        int merged = 0; 
                        RecordReader[] r = null;
                        RecordWriter w = null;
                        Comparator comparator = partitioner.getPartitionComparator();

                        while(arraySplit.size() > 1) {
                                try {
					if (arraySplit.size() > k) {
                                		r = new RecordReader[k];
                                	}
					else {
						r = new RecordReader[arraySplit.size()];
					}

                                	for (int i=0; i<r.length;i++) {
                                        	FileSplit insplit = (FileSplit) arraySplit.remove(0);
       					    	r[i] = ioformat.getRecordReader(TaskStatus.REDUCE);
                                            	r[i].open(configFileName, insplit, reporter);
                                        } 
       				 } catch (Exception e) {
                                        exception = LogError.getStackTrace(e);
                                        reporter.report(0, "ReduceTaskServant::doRun - " +
						"Erro ao preparar RecordReader.\n" + exception);
				 	throw new IOMapReduceException();
				 }
                                 
                                 try {
                                 	FileSplit outsplit = FileSplitHelper.narrow(poa.servant_to_reference(
				                             new FileSplitServant(path[0] + id + "merged" + merged + SUFIX)));
                                        w = ioformat.getRecordWriter(TaskStatus.MAP);
                                        w.open(configFileName, outsplit, reporter);
       				 } catch (Exception e) {
                                        exception = LogError.getStackTrace(e);
                                        reporter.report(0, "ReduceTaskServant::doRun - " +
						"Erro ao preparar RecordWriter.\n" + exception);
				 	throw new IOMapReduceException();
       				 }

                                 merge(r,w);
                                 merged++;
				 arraySplit.add(w.getFileSplit());	
                                 //reporter.report(1,"ReduceTask::run - Finalizado merge:" + w.getFileSplit().getPath());                                  

                                 for (int i=0; i<r.length;i++) {
					r[i].close();
				 }
                                 w.close();
                                 r = null;
                                 w = null; 
			}
				
                        RecordReader input = ioformat.getRecordReader(TaskStatus.REDUCE);
                        FileSplit insplit = (FileSplit) arraySplit.remove(0); 
                        input.open(configFileName, insplit, reporter);

                        AnyHolder key = new AnyHolder();
                        AnyHolder value = new AnyHolder();                       
                        Any last = null;
                        ArrayList<Any> anyList = null;
                                              
                        if (input.next(key,value) ) {
                        	last = key.value;
                                anyList = new ArrayList<Any>();
                                anyList.add(value.value);
                        }
 
                        while(input.next(key,value))
                        {
                        	Any current = key.value;
                             	if (comparator.compare((Object)current,(Object)last)==0) {
			     		anyList.add(value.value);
                                        last = current;
                                }
                             	else {
                                        Any[] values = new Any[anyList.size()];
					anyList.toArray(values);
					reducer.reduce(last, values, collector, reporter);
					values = null;
                                        anyList.clear();
					anyList.add(value.value);
					last = current; 
                        	}
			}
			if (anyList != null && anyList.size() > 0) { 
				Any[] values = new Any[anyList.size()];
				anyList.toArray(values);
				reducer.reduce(last, values, collector, reporter);
				values = null;
                                anyList = null;
                        }
                        collector.flush();
			collector.close();                                               
			input.close();              

                } catch (IOMapReduceException e) {
                        throw e;
		} catch (Exception e) {
                        e.printStackTrace();
			exception = LogError.getStackTrace(e);
                        reporter.report(0, "ReduceTask::doRun - " + exception);
                        throw new IOMapReduceException();
		}
	}        
}


