/*
 * sysstat: System performance tools for Linux
 * (C) 1999-2007 by Sebastien Godard (sysstat <at> orange.fr)
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <time.h>
#include <errno.h>
#include <unistd.h>	/* For STDOUT_FILENO, among others */
#include <dirent.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/resource.h>
#include <sys/utsname.h>


#include "pidstat.h"

/*
 ***************************************************************************
 * Count number of processors in /proc/stat
 ***************************************************************************
 */
int get_proc_cpu_nr(void)
{
   FILE *fp;
   char line[16];
   int num_proc, proc_nr = -1;

   if ((fp = fopen(STAT, "r")) == NULL) {
       return -1;
   }

   while (fgets(line, 16, fp) != NULL) {

      if (strncmp(line, "cpu ", 4) && !strncmp(line, "cpu", 3)) {
	 sscanf(line + 3, "%d", &num_proc);
	 if (num_proc > proc_nr)
	    proc_nr = num_proc;
      }
   }

   fclose(fp);

   return (proc_nr + 1);
}

/*
 ***************************************************************************
 * Read stats from /proc/meminfo to get maxMem
 ***************************************************************************
 */
long read_max_mem()
{
   FILE *fp;
   static char line[128];
   long tlmkb;

   if ((fp = fopen(MEMINFO, "r")) == NULL)
      return -1;

   while (fgets(line, 128, fp) != NULL) {
        if (!strncmp(line, "MemTotal:", 9)){
             /* Read the total amount of memory in kB */
	     sscanf(line + 9, "%lu", &(tlmkb));
             break;
        }
   }
   fclose(fp);
   return tlmkb;
}

/*
 ***************************************************************************
 * Read stats from /proc/meminfo to get freeMem
 ***************************************************************************
 */
long read_free_mem()
{
   FILE *fp;
   static char line[128];
   long frmkb;

   if ((fp = fopen(MEMINFO, "r")) == NULL)
      return -1;

   while (fgets(line, 128, fp) != NULL) {
        if (!strncmp(line, "MemFree:", 8)){
	     /* Read the amount of free memory in kB */
	     sscanf(line + 8, "%lu", &(frmkb));
             break;
        }
   }
   fclose(fp);
   return frmkb;
}

/*
 ***************************************************************************
 * Get time (retorna o tempo em segundos) 
 ***************************************************************************
 */
double get_time()
{
   struct timeval tv;
   
   gettimeofday(&tv, NULL);
   return (double) tv.tv_sec + 1.e-6 * tv.tv_usec;
}

/*
 ***************************************************************************
 * Get system info 
 ***************************************************************************
 */
void get_sys_info(char* buf) {

    struct utsname header;

    /* Get system name, release number and hostname */
    uname(&header);
    strcat(buf, "System name: ");
    strcat(buf, header.sysname);
    strcat(buf, " ");
      
    strcat(buf, "Release: ");
    strcat(buf, header.release);
    strcat(buf, " ");
 
    strcat(buf, "Node name: ");
    strcat(buf, header.nodename);
}

/*
 ***************************************************************************
 * Get process CPU Time 
 ***************************************************************************
 */
long get_process_cpu_time(int nb_cpu){
    int who, rc;
    struct rusage resources;
    double utime, stime;
 
    who = RUSAGE_SELF;
 
    if((rc = getrusage(who, &resources)) != 0) {
        return -1;
    }

    /* user time in seconds */
    utime = (double) resources.ru_utime.tv_sec + 1.e-6 * (double) resources.ru_utime.tv_usec;
    /* kernel time in seconds */
    stime = (double) resources.ru_stime.tv_sec + 1.e-6 * (double) resources.ru_stime.tv_usec;

    /* return total time (user + kernel) in miliseconds */
    return (long) ((utime + stime)*1000 / (nb_cpu));
}

/*
 ***************************************************************************
 * Get process CPU Usage 
 ***************************************************************************
 */

double get_process_cpu_usage(int nb_cpu, double creationTime){
    double nowTime,elapsedTime;    
    int who, rc;   
    struct rusage resources;
    double utime, stime;
   
     nowTime = get_time();
    
    /* time in second */
    elapsedTime = nowTime - creationTime;

    who = RUSAGE_SELF;
    if((rc = getrusage(who, &resources)) != 0) {
         return -1;
    }

    /* user time in seconds */
    utime = (double) resources.ru_utime.tv_sec + 1.e-6 * (double) resources.ru_utime.tv_usec;
    /* kernel time in seconds */
    stime = (double) resources.ru_stime.tv_sec + 1.e-6 * (double) resources.ru_stime.tv_usec;
    
    if ((elapsedTime * 1000) < MIN_ELAPSED_TIME)
        return 0.0;
    else  
        return (double) ((stime + utime) / (nb_cpu * elapsedTime));
}

/*
 ***************************************************************************
 * Read stats from /proc/pid/status to get memory information
 ***************************************************************************
 */
unsigned read_proc_pid_status_total_memory(int pid)
{
     FILE *fp;
     char filename[128], line[256];
     unsigned size;
     //int pid = (unsigned)getpid();  

     sprintf(filename, PID_STATUS, pid);
      
     if ((fp = fopen(filename, "r")) == NULL)
          /* No such process */
          return 0;

     while (fgets(line, 256, fp) != NULL) {

          if (!strncmp(line, "VmSize:", 7)){
	       sscanf(line + 7, "%u", &(size));
               break;
          }
     }

     fclose(fp);     
     return size;
 }

unsigned read_proc_pid_status_resident_memory(int pid)
{
     FILE *fp;
     char filename[128], line[256];
     unsigned resident; 
     //int pid = (unsigned)getpid();  

     sprintf(filename, PID_STATUS, pid);
      
     if ((fp = fopen(filename, "r")) == NULL)
          /* No such process */
          return 0;

     while (fgets(line, 256, fp) != NULL) {

          if (!strncmp(line, "VmRSS:", 6)){
	       sscanf(line + 6, "%u", &(resident));
               break;
          }
     }

     fclose(fp);     
     return resident;
}

/*
 ***************************************************************************
 * Read stats from /proc/diskstats  and proc/self/mountstats
 * to get disk information
 *  all values are cumulative from boot time
 ***************************************************************************
 */
int read_proc_diskstats(struct iostats* sdev){
     FILE *fp;
     char line[256], dev_name[MAX_NAME_LEN];
     int i;
     unsigned long rd_ios, rd_merges_or_rd_sec, rd_ticks_or_wr_sec, wr_ios;
     unsigned long ios_pgr, tot_ticks, rq_ticks, wr_merges, wr_ticks;
     unsigned long long rd_sec_or_wr_ios, wr_sec;
     unsigned int major, minor;

     int sw = 0;
     char nfsline[8192];
     char nfs_name[MAX_NAME_LEN];
     char mount[10], on[10], bytes[10], aux[32];
     unsigned long long rd_normal_bytes, wr_normal_bytes, rd_direct_bytes;
     unsigned long long wr_direct_bytes, rd_server_bytes, wr_server_bytes;
        
     sdev->rd_sectors = 0;
     sdev->rd_ticks   = 0;
     sdev->wr_sectors = 0;
     sdev->wr_ticks   = 0;
     sdev->nfs_rd_sectors =0;
     sdev->nfs_wr_sectors =0;

     if ((fp = fopen(DISKSTATS, "r")) == NULL)
           return -1;

     while (fgets(line, 256, fp) != NULL) {

          i = sscanf(line, "%u %u %s %lu %lu %llu %lu %lu %lu %llu %lu %lu %lu %lu",
	        	 &major, &minor, dev_name,
		         &rd_ios, &rd_merges_or_rd_sec, &rd_sec_or_wr_ios, &rd_ticks_or_wr_sec,
		         &wr_ios, &wr_merges, &wr_sec, &wr_ticks, &ios_pgr, &tot_ticks, &rq_ticks);
           
                    
           /* it is a device, not a partition, and the device is in use*/
           if (i == 14 && rd_ios && wr_ios) {
                sdev->rd_sectors =  sdev->rd_sectors + rd_sec_or_wr_ios;    /* field 3*/
	        sdev->rd_ticks   =  sdev->rd_ticks + rd_ticks_or_wr_sec;    /* field 4*/
	        sdev->wr_sectors =  sdev->wr_sectors + wr_sec;              /* field 7*/
	        sdev->wr_ticks   =  sdev->wr_ticks + wr_ticks;              /* field 8*/
           }
     }
     fclose(fp);
     
     /* get nfs information*/
     if ((fp = fopen(NFSMOUNTSTATS, "r")) == NULL)
          return 0;
     
     sprintf(aux, "%%%ds %%10s %%10s",
	   MAX_NAME_LEN < 200 ? MAX_NAME_LEN : 200);

     while (fgets(nfsline, 256, fp) != NULL) {
            
            /* read NFS directory name */
            if (!strncmp(nfsline, "device", 6)) {
                   sw = 0;
                   sscanf(nfsline + 6, aux, nfs_name, mount, on);
                   if ((!strncmp(mount, "mounted", 7)) && (!strncmp(on, "on", 2))) {
                         sw = 1;
                   }
             }

             sscanf(nfsline, "%10s", bytes);
             if (sw && (!strncmp(bytes, "bytes:", 6))) {
                      /* Read the stats for the last NFS-mounted directory */
                      i = sscanf(strstr(nfsline, "bytes:") + 6, "%llu %llu %llu %llu %llu %llu",
		                   &rd_normal_bytes, &wr_normal_bytes, &rd_direct_bytes,
		                   &wr_direct_bytes, &rd_server_bytes, &wr_server_bytes);
                       
                       sdev->nfs_rd_sectors = sdev->nfs_rd_sectors + rd_normal_bytes;
                       sdev->nfs_wr_sectors = sdev->nfs_wr_sectors + wr_normal_bytes;
                       sw = 0;
             }
      }
      fclose(fp);
      return 0;
}

/*
 ***************************************************************************
 * Read stats from /proc/net/dev to get network information
 *  all values are cumulative from boot time
 ***************************************************************************
 */
int read_proc_net_dev(struct netstats* snet){
      FILE *fp;
      char line[2048];
      unsigned long long  v_received, v_2, v_3, v_4, v_5, v_6, v_7, v_8, v_transmitted, 
                          v_10, v_11, v_12, v_13, v_14, v_15, v_16;
      
      if ((fp = fopen(NET, "r")) == NULL) {
          return -1;
      }
      
      snet->by_received = 0;
      snet->by_transmitted = 0;

      while (fgets(line, 2048, fp) != NULL) {

              if (!strncmp(line, "  eth", 5)) {
	          sscanf(line + 7, "%llu %llu %llu %llu %llu %llu %llu %llu %llu %llu %llu %llu %llu %llu %llu %llu",
                                    &v_received, &v_2, &v_3, &v_4, &v_5, &v_6, &v_7, &v_8, &v_transmitted, 
                                     &v_10, &v_11, &v_12, &v_13, &v_14, &v_15, &v_16);
	          
                  snet->by_received = snet->by_received + v_received;
                  snet->by_transmitted = snet->by_transmitted + v_transmitted;
              }
       }
       fclose(fp);
       return 0;
}
 



