package scs.demos.mapreduce.user;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import org.omg.CORBA.AnyHolder;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.RecordReaderPOA;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.schedule.LogError;

/**
 * Classe que implementa a interface RecordReader, retornando os token lidos de um fileSplit.
 * Deve ser inicializado com o nome do arquivo de configuracão e o FileSplit associado 
 * @author Sand Luz Correa
*/

public class TokenRecordReader extends RecordReaderPOA {

        private FileSplit fileSplit = null;
        private int bufferSize;

	private final int BUFFER_SIZE = 100000; //100K;
	private final char SEPARATOR = ' ';
        private final String KEY = "none";
        private Properties config = null;  

        private char[] buff = null;
        private char[] buffWord = new char[1000];
        private String word = null;
        private int pos = 0;
        private int index = 0;
        private int size;

        private InputStream in = null;
        private BufferedReader br = null;
        private boolean lastReturned = false;
        private Reporter reporter = null;
	private ORB orb;

        private boolean opened = false;
        private String exception;
        private Any keyAux = null;
        private Any valueAux = null;


	/* retorna -1 se eof
         * retorna 0 caso contrario 
         */
        private int read() throws java.io.IOException {
              int ret;
              try {
                        index = 0;
                        word = null;

                        while (!lookup()) { 
                                if ((ret = br.read(buff,0,bufferSize)) ==-1) {
                                    if (index > 0) {
                                        word = String.copyValueOf(buffWord,0,index);
                                    }
                                    return ret;
                                }
                                else {   
                                        size = ret; 
					pos = 0;  
                                }
                        }

                        word = String.copyValueOf(buffWord,0,index);                     
                        return 0;                         
		} catch (java.io.IOException e) {
                  	throw e;
		}
	}

 	private boolean lookup() {
		while (pos < size) {

                       if (buff[pos] != SEPARATOR) {
                       		if (index < buffWord.length) {
                                	buffWord[index] = buff[pos];
                                      	index++;
                            	}
                            	else {
                                      return true;
                            	}
                        }	
                        else {
                                 while(pos < size && buff[pos]==SEPARATOR) {
                                       pos++;
                                 }
                                 if (pos < size || index > 0) {
					return true;
                                 }
                         }
                         pos++;
		}
		return false;
	}

	public void open(String confFileName, FileSplit fileSplit, Reporter reporter) throws scs.demos.mapreduce.IOMapReduceException {
        	try {
        		if (opened) {
        	        	return;
        	        }
        	            
			this.reporter = reporter;
                        this.fileSplit = fileSplit;
                        this.config = new Properties();
                             
			orb = ORB.init();
                        
			config.load(new FileInputStream(confFileName));
			bufferSize = Integer.valueOf(this.config.getProperty("mapred.RecordReader.buffer-size"));

                        if (bufferSize == 0) {
	                        bufferSize = BUFFER_SIZE;
			}
                   
 			keyAux = orb.create_any();
                        valueAux = orb.create_any();

			opened = true;                         
		} catch (Exception e) {
			exception = LogError.getStackTrace(e);
                        reporter.report(0,"TokenRecordReader::open -" + exception);
	        	throw new scs.demos.mapreduce.IOMapReduceException();
           	 }	
	}
	
	public boolean next(AnyHolder key, AnyHolder value) throws scs.demos.mapreduce.IOMapReduceException {
        	try {
                        if (!opened) {
				return false;
			}
                        
                        if (buff == null) {
                        	in = new FileInputStream(fileSplit.getPath());
                        	br = new BufferedReader(new InputStreamReader(in));
                           	buff  = new char[bufferSize];
                           	pos = 0;
				size = br.read(buff,0,bufferSize);
			}
                        
                        int r = read();
			if ((r == 0) || (word != null && !lastReturned)) {
				//reporter.report(1,"word: " + word);
				String split[] = word.split("\\|");

                            	if (split.length==1) {
					keyAux.insert_string(KEY);
                                	valueAux.insert_string(word); 	
               			}
				else {
					keyAux.insert_string(split[0]);
                                	valueAux.insert_string(split[1]);
				}
							                
                            	if ((r < 0) && (word != null) && (!lastReturned)) {
					lastReturned = true;
				}

				key.value = keyAux ;
                            	value.value = valueAux;
				//reporter.report(1, "retornando key:" + key.value.extract_string());
				//reporter.report(1, "retornando value:" + value.value.extract_string());
				return true;  
			}
			else {
			        keyAux.insert_string(KEY);
                                valueAux.insert_string(KEY); 	
				key.value = keyAux ;
                                value.value = valueAux;  
                                return false;
			}
               } catch (Exception e) {
                    	exception = LogError.getStackTrace(e);
                        reporter.report(0,"TokenRecordReader::open -" + exception);
               		throw new scs.demos.mapreduce.IOMapReduceException();
	       }	
        }
         
        public void close() throws scs.demos.mapreduce.IOMapReduceException{
                try{
                        if (!opened) {
                            return;
                        }
                        
                        in.close();
                        br.close();
                        
                        opened = false;
                } catch (Exception e){
                	exception = LogError.getStackTrace(e);
                        reporter.report(0,"TokenRecordReader::open -" + exception);
			throw new scs.demos.mapreduce.IOMapReduceException();
 		}
	}

	public FileSplit getFileSplit() {
		return fileSplit;
	}
}
