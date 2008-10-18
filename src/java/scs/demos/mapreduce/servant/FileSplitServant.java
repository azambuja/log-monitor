package scs.demos.mapreduce.servant;

import java.io.File;

import scs.demos.mapreduce.FileSplit;
import scs.demos.mapreduce.FileSplitPOA;

/**
 * Servant que implementa a interface scs::demos::mapreduce::FileSplit
 * Representa uma particao do arquivo de entrada
 * @author Sand Luz Correa
*/

public class FileSplitServant extends FileSplitPOA {

        private File file;		

	public FileSplitServant (String path){
		this.file = new File(path);
	}

	public String getPath() {
		return file.getPath();
	}

        public long getLength() {
                return file.length();
	}
	
}
	

