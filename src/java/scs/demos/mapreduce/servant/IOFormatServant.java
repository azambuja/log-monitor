package scs.demos.mapreduce.servant;

import java.io.*;
import java.util.Properties;
import java.util.ArrayList;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;
import org.omg.PortableServer.Servant;

import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.RecordReader;
import scs.demos.mapreduce.RecordWriter;
import scs.demos.mapreduce.IOFormatPOA;
import scs.demos.mapreduce.SplitException;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.servant.FileSplitServant;
import scs.demos.mapreduce.FileSplitHelper;
import scs.demos.mapreduce.IOFormatException;
import scs.demos.mapreduce.schedule.LogError;
import scs.demos.mapreduce.TaskStatus;


/**
 * Servant generico que implementa uma interface IOFormat. Serve de classe abstrata para 
 * outros IOFormat
 * 
 */
public abstract class IOFormatServant extends IOFormatPOA {
   	/**
	* Tamanho default para a propriedade mapred.FileSplit.size
	* (tamanho de um split do arquivo de entrada) 
	*/
	private final long FILE_SPLIT_SIZE = 2048;
    	private long fileSplitSize;
    	private String inputName;
	protected POA poa = null;
    	private ORB orb = null; 
    	protected String exception;
      
        public IOFormatServant() throws Exception {
		String[] args = new String[1];
                args[0] = "inicio";	     
                this.orb = ORB.init(args, null);

                this.poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
		this.poa.the_POAManager().activate();
	} 

	/**
	 * Metodo abstrato para retornar um RecordReader
	 * @return RecordReader 
	 */
	protected abstract RecordReader doGetRecordReader(TaskStatus status) throws Exception;

	/**
	 * Metodo abstrato para retornar um RecordWriter
	 * @return RecordWriter
	 */
	protected abstract RecordWriter doGetRecordWriter(TaskStatus status) throws Exception;

    	/**
	 * Metodo que particiona o arquivo de entrada em vários segmentos menores, 
     	 * de acordo com o especificado no arquivo de configuracao
	 * @return FileSptlit[]
	 */
	public FileSplit[] getSplits(String confFileName, Reporter reporter) throws SplitException {
		try {
				        
        		Properties config = new Properties();
       			config.load(new FileInputStream(confFileName));
       			reporter.report(2, "IOFormatServant::getSplits - confFileName: " + confFileName);

       			fileSplitSize = Long.valueOf(config.getProperty("mapred.Input.split-size"));
                	if (fileSplitSize == 0) {
              			fileSplitSize = FILE_SPLIT_SIZE;
			}

			inputName = config.getProperty("mapred.Input.name");
                	if (inputName == null) {
              			reporter.report(0,"IOFormatServant::getSplits - Parametro mapred.Input.name nao fornecido");
  				throw new SplitException();
			}
		
		        DataInputStream in = new DataInputStream(new
		                             BufferedInputStream(new FileInputStream(inputName)));
		      	reporter.report(1,"IOFormatServant::getSplits - Iniciando o particionamento do arquivo");
                        
                	int i = 0;
                	boolean ended = false;
                	ArrayList<FileSplit> splits = new ArrayList<FileSplit> ();
                	FileSplit f = null;

                	while( (f = getSplit(in, i, reporter)) != null) {
				splits.add(f);
                   		i++;
                	}
		     
                	return splits.toArray(new FileSplit[splits.size()]);
		} catch (Exception e) {
  			exception = LogError.getStackTrace(e);
                        reporter.report(0, "IOFormatServant::getSplits - " + exception); 
			throw new SplitException();		
		}
	}

    	public RecordReader getRecordReader(TaskStatus status) throws IOFormatException{
        	try {
    			RecordReader r = doGetRecordReader(status);
		        return r; 
		} catch (Exception e) {
			exception = LogError.getStackTrace(e);
			throw new IOFormatException ("IOFormatServant::getRecordReader - " + exception);
		}
	}

	public RecordWriter getRecordWriter(TaskStatus status) throws IOFormatException{
        	try{
			RecordWriter w = doGetRecordWriter(status);
           		 return w;
        	} catch (Exception e) {
             		exception = LogError.getStackTrace(e);
                        throw new IOFormatException ("IOFormatServant::getRecordWriter - " + exception);
		} 
	}
	
	private FileSplit getSplit(DataInputStream in, int i, Reporter reporter) throws IOException {
        	try {
                 	byte[] read = new byte[(int) fileSplitSize];
        		int nread = in.read(read,0,(int) fileSplitSize);
	               
        		if (nread <= 0) {
               			return null;
	       		}	
                
                	String[] split = inputName.split(".txt");
	       		String path = split[0] + i + ".txt";
                        		        
			reporter.report(1,"IOFormatServant::getSplit - Criando split: " + path);
	                DataOutputStream out = new DataOutputStream(new
				               	BufferedOutputStream(new FileOutputStream(path)));
	                out.write(read,0,nread);

		        byte[] nextByte = new byte [1];
		        nextByte[0] = read[nread -1];
                    	while(nextByte[0] != ' ') { 
                    		if (in.read(nextByte,0,1) > 0) 
                              		out.write(nextByte[0]);
                            	else
					break;
                    	}
		        out.flush();
		        out.close();
                        
                    	return FileSplitHelper.narrow(poa.servant_to_reference(new FileSplitServant(path)));
		} catch (Exception e) {
		    	exception = LogError.getStackTrace(e);
			reporter.report(0, "IOFormatServant::getSplit - " + exception); 
			throw new IOException();
		}
	}
}
