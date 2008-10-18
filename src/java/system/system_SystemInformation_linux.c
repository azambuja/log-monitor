/* ------------------------------------------------------------------------- */
/*
 * A Linux implementation of JNI methods in com.vladium.utils.SystemInformation
 * class. 
 *
 * For simplicity, this implementaion assumes JNI 1.2+ and omits error handling.
 *
 * Enhanced by Sand Luz Correa (C) 2008, 
 * Original source (C) 2002, Vladimir Roubtsov [vlad@trilogy.com]
 */
/* ------------------------------------------------------------------------- */

#include <stdio.h>
#include "pidstat.h"
#include "system_SystemInformation.h"


static jint s_PID;
static int alreadyDetached;
static int externalCPUmon;
static int s_numberOfProcessors;
static double creationTime;


/* ------------------------------------------------------------------------- */

/*
 * This method was added in JNI 1.2. It is executed once before any other
 * methods are called and is ostensibly for negotiating JNI spec versions, but
 * can also be conveniently used for initializing variables that will not
 * change throughout the lifetime of this process.
 */
JNIEXPORT jint JNICALL
JNI_OnLoad (JavaVM * vm, void * reserved)
{
        
      s_PID = (jint)((unsigned) getpid());
      s_numberOfProcessors = get_proc_cpu_nr();
      creationTime = get_time ();
      alreadyDetached = 0;
      externalCPUmon = 0;

      return JNI_VERSION_1_2;
}
/* ......................................................................... */

JNIEXPORT void JNICALL
JNI_OnUnload (JavaVM * vm, void * reserved)
{        
       
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getCPUs
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_system_SystemInformation_getCPUs (JNIEnv * env, jclass cls)
{
      return (jint) s_numberOfProcessors;
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getSysInfo
 * Signature: ()S
 */
JNIEXPORT jstring JNICALL
Java_system_SystemInformation_getSysInfo (JNIEnv * env, jclass cls)
{
      char buf[2048];
      *buf=0;
      get_sys_info(buf);        
      jstring retval = (*env)->NewStringUTF(env,buf);
      free(buf);
      return retval;
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getProcessID
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_system_SystemInformation_getProcessID (JNIEnv * env, jclass cls)
{
      return s_PID;
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    setPid
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_system_SystemInformation_setPid (JNIEnv * env, jclass cls, jint pid)
{
      s_PID = pid;
      externalCPUmon = 1;
      printf("[CPUmon] Attached to process. \n");
      fflush(stdout);
      return 0;
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    detachProcess
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_system_SystemInformation_detachProcess (JNIEnv * env, jclass cls)
{
      if (externalCPUmon && !alreadyDetached) {
 	  alreadyDetached = 1;
          printf("[CPUmon] Detached from process. \n");
          fflush(stdout);
      }
    
      return 0;
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getProcessCPUTime
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_system_SystemInformation_getProcessCPUTime (JNIEnv * env, jclass cls)
{
    return (jlong) get_process_cpu_time(s_numberOfProcessors);
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getMaxMem
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_system_SystemInformation_getMaxMem (JNIEnv * env, jclass cls)
{
     return (jlong) read_max_mem();
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getFreeMem
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_system_SystemInformation_getFreeMem (JNIEnv * env, jclass cls)
{
        return (jlong) read_free_mem();
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getProcessCPUUsage
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_system_SystemInformation_getProcessCPUUsage (JNIEnv * env, jclass cls)
{
    return (jdouble) get_process_cpu_usage(s_numberOfProcessors,creationTime);
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getProcessCPUPercentage
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_system_SystemInformation_getProcessCPUPercentage (JNIEnv * env, jclass cls)
{
	// Not implemented on Windows
        return (jdouble)(-1.0);
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getMemoryUsage
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_system_SystemInformation_getMemoryUsage (JNIEnv * env, jclass cls)
{
    return (jlong) read_proc_pid_status_total_memory((int) s_PID);
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getMemoryResident
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_system_SystemInformation_getMemoryResident (JNIEnv * env, jclass cls)
{
     return (jlong) read_proc_pid_status_resident_memory((int) s_PID);
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getNativeIOUsage
 * Signature: ()J
 */

JNIEXPORT jlongArray JNICALL 
Java_system_SystemInformation_getNativeIOUsage (JNIEnv * env, jclass cls)
{
      struct iostats s_iostats;
      jlong ioMetrics[6];
      jlongArray elemArr;

      int result = read_proc_diskstats(&s_iostats);
      if(result == -1)
          return NULL;

      ioMetrics[0] =  (jlong) s_iostats.rd_sectors;
      ioMetrics[1] =  (jlong) s_iostats.rd_ticks;
      ioMetrics[2] =  (jlong) s_iostats.wr_sectors;
      ioMetrics[3] =  (jlong) s_iostats.wr_ticks;
      ioMetrics[4] =  (jlong) s_iostats.nfs_rd_sectors;
      ioMetrics[5] =  (jlong) s_iostats.nfs_wr_sectors;

      elemArr = (*env)->NewLongArray(env, 6);
      if (elemArr == NULL) {
            return NULL; /* exception thrown */
      }
      (*env)->SetLongArrayRegion(env, elemArr, 0, 6, ioMetrics);

      return elemArr ;
}
/* ......................................................................... */

/*
 * Class:     system_SystemInformation
 * Method:    getNativeNetworkUsage
 * Signature: ()J
 */

JNIEXPORT jlongArray JNICALL
Java_system_SystemInformation_getNativeNetworkUsage (JNIEnv * env, jclass cls)
{
      struct netstats s_netstats;
      jlong netMetrics[2];
      jlongArray elemArr;

      int result =  read_proc_net_dev(&s_netstats);
      if(result == -1)
              return NULL;

      netMetrics[0] =  (jlong) s_netstats.by_received;
      netMetrics[1] =  (jlong) s_netstats.by_transmitted;
      
      elemArr = (*env)->NewLongArray(env, 2);
      if (elemArr == NULL) {
            return NULL; /* exception thrown */
      }
      (*env)->SetLongArrayRegion(env, elemArr, 0, 2, netMetrics);

      return elemArr ;
}

/* ------------------------------------------------------------------------- */
/* end of file */

