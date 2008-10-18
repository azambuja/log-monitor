package system;

import system.SystemInformation.NegativeCPUTime;
import system.SystemInformation.IOStats;
import system.SystemInformation.NetworkStats;
import java.io.*;

public class Test
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		SystemInformation.CPUUsageSnapshot start = null;
		SystemInformation.CPUUsageSnapshot end = null;
		String file = "/home/prj/openbus/work/scs_healing/sand/src/java/scs/demos/mapreduce/dataset/teste100M0.txt";

	       try
		{
			System.out.println("processID: " + SystemInformation.getProcessID());
                        
                        IOStats io = SystemInformation.getIOUsage();
                        NetworkStats net = SystemInformation.getNetworkUsage();
                        System.out.println("io:");
                        System.out.println("sector read:" + io.sectors_read);
                        System.out.println("sector writen:" + io.sectors_writen);
                        System.out.println("sector read nfs:" + io.sectors_read_in_nfs);
                        System.out.println("sector write nfs:" + io.sectors_writen_in_nfs);
                        System.out.println("read + write:" + (io.sectors_read + io.sectors_writen));
                        System.out.println("bytes received:" + net.bytes_received);
                       	System.out.println("bytes transmitted:" + net.bytes_transmitted);
                        
                        long memorySize = SystemInformation.getMemoryUsage();
                        System.out.println("memoria antes da alocacao:" + memorySize);

			char[] buff = new char[1000000];
                        FileInputStream in = new FileInputStream(file);
                        BufferedReader br = new BufferedReader(new InputStreamReader(in),1000000);
                        int ret = br.read(buff,0,1000000);
                
                        start = SystemInformation.makeCPUUsageSnapshot();        
                        System.out.println("cputime antes de lacos:" + start.m_CPUTime);
        
			long i = 0;
			while (i < 1000000000) i++;
			i=0;
			while (i < 1000000000) i++;
			i=0;
			while (i < 1000000000) i++;
			i=0;
			while (i < 1000000000) i++;
			i=0;
			while (i < 1000000000) i++;
	
			end = SystemInformation.makeCPUUsageSnapshot();
                        System.out.println("cputime depois de laco:" + end.m_CPUTime);           
			System.out.println("diferenca:" + (end.m_CPUTime - start.m_CPUTime));
			
			memorySize = SystemInformation.getMemoryUsage();
                        System.out.println("memoria depois da alocacao:" + memorySize);
    
			double receivedCPUUsage = 
				100.0 * SystemInformation.getProcessCPUUsage(start, end);
			System.out.println("Current CPU usage is " + receivedCPUUsage + "%");

                        double avgCPUUsage = SystemInformation.getProcessCPUUsage();
                        System.out.println("avg CPU usage is: " + avgCPUUsage);

                        Thread.sleep(10000);

                        IOStats io2 = SystemInformation.getIOUsage();
                        net = SystemInformation.getNetworkUsage();
                        System.out.println("io:");
                        System.out.println("sector read:" + io2.sectors_read);
                        System.out.println("sector writen:" + io2.sectors_writen);
                        System.out.println("sector read nfs:" + io2.sectors_read_in_nfs);
                        System.out.println("sector write nfs:" + io2.sectors_writen_in_nfs);
                        System.out.println("read + write:" + (io2.sectors_read + io2.sectors_writen));
                        System.out.println("bytes received:" + net.bytes_received);
                        System.out.println("bytes transmitted:" + net.bytes_transmitted);

			/*Runnable runnable = new Runnable() 
		        {
	                      public void run() 
		              {
                                 try{
			         long l =0;
                                 final SystemInformation.CPUUsageSnapshot start1;
                                 final SystemInformation.CPUUsageSnapshot end1;
                                 start1 = SystemInformation.makeCPUUsageSnapshot();
                                 while (l < 1000000) l++;
                                 end1 = SystemInformation.makeCPUUsageSnapshot();
                                 System.out.println(end1.m_CPUTime);
                                 System.out.println(start1.m_CPUTime);
                                 } catch (Exception e) { }
                              
                              }
		        };
		        Thread runner = new Thread(runnable);
		        runner.start();*/            
                        		
		}
		catch (NegativeCPUTime e)
		{
			e.printStackTrace();
		}
                catch (Exception e) {
                        e.printStackTrace();
                }
	}
}
