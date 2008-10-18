package system;

public abstract class SystemInformation
{
	// public: ................................................................

	/**
	 * A simple class to represent data snapshots taken by
	 * {@link #makeCPUUsageSnapshot}.
	 */
	public static final class CPUUsageSnapshot
	{
		public final long m_time, m_CPUTime;

		// constructor is private to ensure that makeCPUUsageSnapshot()
		// is used as the factory method for this class:
		private CPUUsageSnapshot(final long time, final long CPUTime)
		{
			m_time = time;
			m_CPUTime = CPUTime;
		}

	} // end of nested class

	// Custom exception class for throwing
	public static final class NegativeCPUTime extends Exception
	{
		private static final long serialVersionUID = 1L;
	}

	/**
	 * A class to represent io data snapshots
	 */
        public static final class IOStats
        {
                public final long sectors_read;           /* number of sectors read since boot time*/
                public final long time_spent_in_readings; /* number of milisseconds spent with readings, since boot time*/
                public final long sectors_writen;         /* number of sectors writen since boot time*/
                public final long time_spent_in_writings; /* number of milisseconds spent with writings, since boot time*/
                public final long sectors_read_in_nfs;    /* number of sectors read from network file system*/ 
                public final long sectors_writen_in_nfs;  /* number of sectors writen to network file system*/

                private IOStats(final long v1, final long v2, final long v3, final long v4, final long v5, final long v6)
                { 
                      sectors_read = v1;
                      time_spent_in_readings = v2;
                      sectors_writen = v3;
                      time_spent_in_writings = v4;
                      sectors_read_in_nfs = v5;
                      sectors_writen_in_nfs = v6;
                }
         }

	/**
	 * A class to represent network snapshots
	 */
         public static final class NetworkStats
         {
                public final long bytes_received;
                public final long bytes_transmitted;
                
                private NetworkStats(final long v1, final long v2)
                {
                       bytes_received = v1;
                       bytes_transmitted = v2;
                }
          }

	/**
	 * Minimum time difference [in milliseconds] enforced for the inputs into
	 * {@link #getProcessCPUUsage(SystemInformation.CPUUsageSnapshot,SystemInformation.CPUUsageSnapshot)}.
	 * The motivation for this restriction is the fact that
	 * System.currentTimeMillis() on some systems has a low resolution (e.g.,
	 * 10ms on win32). The current value is 100 ms.
	 */
	public static final int MIN_ELAPSED_TIME = 100;

	/**
	 * Creates a CPU usage data snapshot by associating CPU time used with
	 * system time. The resulting data can be fed into
	 * {@link #getProcessCPUUsage(SystemInformation.CPUUsageSnapshot,SystemInformation.CPUUsageSnapshot)}.
	 */
	public static CPUUsageSnapshot makeCPUUsageSnapshot()
			throws SystemInformation.NegativeCPUTime
	{
		long prCPUTime = getProcessCPUTime();
		if (prCPUTime < 0) throw new NegativeCPUTime();
		return new CPUUsageSnapshot(System.currentTimeMillis(),
				getProcessCPUTime());
	}

        /**
	 * Creates an io usage data snapshot
	 */
        public static IOStats getIOUsage()
        {
                IOStats  iostatsObj;
                long[] ioMetrics;
                ioMetrics = getNativeIOUsage();

                if (ioMetrics==null)
                    return null;

                iostatsObj = new IOStats(ioMetrics[0], ioMetrics[1], ioMetrics[2], 
                                         ioMetrics[3], ioMetrics[4], ioMetrics[5]);
                return iostatsObj;
        } 

	/**
	 * Creates a network usage snapshot
	 */
        public static NetworkStats getNetworkUsage()
        {
                NetworkStats  netstatsObj;
                long[] netMetrics;
                netMetrics = getNativeNetworkUsage();
                netstatsObj = new NetworkStats(netMetrics[0], netMetrics[1]);
                return netstatsObj;
        } 
 
    
	/**
	 * Computes CPU usage (fraction of 1.0) between start.m_CPUTime and
	 * end.m_CPUTime time points [1.0 corresponds to 100% utilization of all
	 * processors].
	 * 
	 * @throws IllegalArgumentException
	 *             if start and end time points are less than
	 *             {@link #MIN_ELAPSED_TIME} ms apart.
	 * @throws IllegalArgumentException
	 *             if either argument is null;
	 */
	public static double getProcessCPUUsage(final CPUUsageSnapshot start,
			final CPUUsageSnapshot end)
	{
		if (start == null)
			throw new IllegalArgumentException("null input: start");
		if (end == null) throw new IllegalArgumentException("null input: end");
		if (end.m_time < start.m_time + MIN_ELAPSED_TIME)
			throw new IllegalArgumentException("end time must be at least "
					+ MIN_ELAPSED_TIME + " ms later than start time");

		return ((double) (end.m_CPUTime - start.m_CPUTime))
				/ (end.m_time - start.m_time);
	}

        /**
	 * Returns the PID of the current process. The result is useful when you
	 * need to integrate a Java app with external tools.
	 */
	public static native int getProcessID();

	/**
	 * Returns the number of processors on machine
	 */
	public static native int getCPUs();

	/**
	 * Returns CPU (kernel + user) time used by the current process [in
	 * milliseconds]. The returned value is adjusted for the number of
	 * processors in the system.
	 */
	public static native long getProcessCPUTime();

	/**
	 * Returns CPU (kernel + user) time used by the current process [in
	 * perecents]. The returned value is either CPU percentage, or zero if this
	 * is not supported by OS. Currently it is supported by Solaris8, and not
	 * supported by Windows XP
	 */
	public static native double getProcessCPUPercentage();

	/**
	 * Returns maximum memory available in the system.
	 */
	public static native long getMaxMem();

	/**
	 * Returns current free memory in the system.
	 */
	public static native long getFreeMem();

	/**
	 * Returns system name info like "uname" command output
	 */
	public static native String getSysInfo();

	/**
	 * Returns CPU usage (fraction of 1.0) so far by the current process. This
	 * is a total for all processors since the process creation time.
	 */
	public static native double getProcessCPUUsage();

	/**
	 * Returns current space allocated for the process, in Kbytes. Those pages
	 * may or may not be in memory.
	 */
	public static native long getMemoryUsage();

	/**
	 * Returns current process space being resident in memory, in Kbytes.
	 */
	public static native long getMemoryResident();

	/**
	 * Sets the system native process PID for which all measurements will be
	 * done. If this method is not called then the current JVM pid will act as a
	 * default. Returns the native-dependent error code, or 0 in case of
	 * success.
	 */
	public static native int setPid(int pid);

	/**
	 * Closes native-dependent process handle, if necessary.
	 */
	public static native int detachProcess();

        public static native long[] getNativeIOUsage();

        public static native long[] getNativeNetworkUsage();

	// protected: .............................................................

	// package: ...............................................................

	// private: ...............................................................

	private SystemInformation()
	{
	} // prevent subclassing

	private static final String SILIB = "system_SystemInformation";

	static
	{
		// loading a native lib in a static initializer ensures that it is
		// available done before any method in this class is called:
		try
		{
			System.loadLibrary(SILIB);
		}
		catch (UnsatisfiedLinkError e)
		{
			System.out.println("native lib '" + SILIB
					+ "' not found in 'java.library.path': "
					+ System.getProperty("java.library.path"));

			throw e; // re-throw
		}
	}

} // end of class

