package scs.demos.mapreduce.user;

import scs.demos.mapreduce.servant.IOFormatServant;
import scs.demos.mapreduce.RecordReader;
import scs.demos.mapreduce.RecordReaderHelper;
import scs.demos.mapreduce.RecordWriter;
import scs.demos.mapreduce.RecordWriterHelper;
import scs.demos.mapreduce.schedule.LogError;
import scs.demos.mapreduce.TaskStatus;



/**
 * Servant que estende o servant gernerico IOFormatServant 
 * 
 */
public class TokenIOFormat extends IOFormatServant {
        
        public TokenIOFormat() throws Exception{
		super();
		
	}
	protected RecordReader doGetRecordReader(TaskStatus status) throws Exception {
               	try {
			return RecordReaderHelper.narrow(poa.servant_to_reference(new TokenRecordReader()));
               	} catch (Exception e) {
                        throw e;
		}
	}

	protected RecordWriter doGetRecordWriter(TaskStatus status) throws Exception {
                try {
			return RecordWriterHelper.narrow(poa.servant_to_reference(new TokenRecordWriter()));
 		} catch (Exception e) {
 			throw e;
		}
	}
}

