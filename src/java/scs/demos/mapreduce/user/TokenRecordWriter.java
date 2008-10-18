package scs.demos.mapreduce.user;

import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Properties;
import org.omg.CORBA.Any;

import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.IOMapReduceException;
import scs.demos.mapreduce.RecordWriterPOA;
import scs.demos.mapreduce.Reporter;
import scs.demos.mapreduce.schedule.LogError;

/**
 * Classe que implementa a interface RecordWriter, escrevendo tokens para um fileSplit.
 * Deve ser inicializado com o nome do arquivo de configuracão e o FileSplit associado 
 * @author Sand Luz Correa
*/

public class TokenRecordWriter extends RecordWriterPOA{
	private FileSplit fileSplit = null;
	private int bufferSize;

	private final int BUFFER_SIZE = 100000; //100K;
	private final char SEPARATOR = ' ';

	private Properties config = null;  
	
    	private char[] buff = null;
    	private char[] buffWord;
    	private int pos = 0;
    	private int index = 0;
    	private OutputStream out = null;
    	private BufferedWriter bw = null;
    	private String file;
    	private int size;
    	private Reporter reporter = null;

    	private boolean opened = false;
    	private String exception;

	public void open(String confFileName, FileSplit fileSplit, Reporter reporter) throws scs.demos.mapreduce.IOMapReduceException {
        	try {
        	        if (opened) {
        	        	return;
        	        }
        	
                	this.reporter = reporter;
                    	this.fileSplit = fileSplit;
                   	this.config = new Properties();
                        
	                config.load(new FileInputStream(confFileName));
	                bufferSize = Integer.valueOf(this.config.getProperty("mapred.RecordWriter.buffer-size"));

                    	if (bufferSize == 0) {
                        	bufferSize = BUFFER_SIZE;
			} 
					
			if (buff == null) {
                        	out = new FileOutputStream(fileSplit.getPath());
                        	bw = new BufferedWriter(new OutputStreamWriter(out));
                            	buff  = new char[bufferSize];
                           	pos = 0;
			}			
                    	opened = true;	                       
		                         
		} catch (IOException e) {
			exception = LogError.getStackTrace(e);
	        	throw new scs.demos.mapreduce.IOMapReduceException(exception);
            	}	
	}
 
	public boolean write(Any key, Any value) throws scs.demos.mapreduce.IOMapReduceException {
              try {     
                        if (!opened) {
				return false;
			}
		
			String s = key.extract_string() + "|" + value.extract_string() + " ";
                        buffWord = s.toCharArray();
						
                        if (buffWord.length > (bufferSize - pos)) {
                        	bw.write(buff,0,pos);
                            	bw.flush();
                           	pos = 0; 
                        }

                        System.arraycopy(buffWord,0,buff,pos,buffWord.length);
                        pos = pos + buffWord.length;
                        return true;
		} catch (Exception e) {
		        exception = LogError.getStackTrace(e);
                        reporter.report(0,"TokenRecordWriter::write - " + exception);
			throw new scs.demos.mapreduce.IOMapReduceException();
		}
	}
        
	public void close()throws scs.demos.mapreduce.IOMapReduceException {
               try {
                        if (!opened) {
                		return;
                        }
                        
			if (pos>0) {
				bw.write(buff,0,pos);
                        	bw.flush();
			} 
			out.close();
			bw.close();
			opened = false;
		} catch (Exception e){
		        exception = LogError.getStackTrace(e); 
                        reporter.report(0,"TokenRecordWriter::write - " + exception);
			throw new scs.demos.mapreduce.IOMapReduceException();
		}
	}

   	public FileSplit getFileSplit() {
		return fileSplit;
	}
}
