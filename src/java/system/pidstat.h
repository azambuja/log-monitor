/*
 * sysstat: System performance tools for Linux
 * (C) 1999-2007 by Sebastien Godard (sysstat <at> orange.fr)
 */

#ifndef _PIDSTAT_H
#define _PIDSTAT_H

#include <time.h>

/* Files */
#define STAT		"/proc/stat"
#define MEMINFO		"/proc/meminfo"
#define PID_STATUS	"/proc/%u/status"
#define DISKSTATS	"/proc/diskstats"
#define NFSMOUNTSTATS	"/proc/self/mountstats"
#define NET      	"/proc/net/dev"


#define MIN_ELAPSED_TIME (10)    /* ms */
#define MAX_NAME_LEN	72


struct iostats{
     unsigned long long rd_sectors;
     unsigned long rd_ticks;
     unsigned long long wr_sectors;
     unsigned long wr_ticks;
     unsigned long long nfs_rd_sectors;
     unsigned long long nfs_wr_sectors;
};

struct netstats{
     unsigned long long by_received;
     unsigned long by_transmitted;
};

/* Functions */
extern int get_proc_cpu_nr(void);
extern long read_max_mem(void);
extern long read_free_mem(void);
extern double get_time(void);
extern void get_sys_info(char*);
extern long get_process_cpu_time(int);
extern double get_process_cpu_usage(int, double);
extern unsigned read_proc_pid_status_total_memory(int);
unsigned read_proc_pid_status_resident_memory(int);
int read_proc_diskstats(struct iostats*);
int read_proc_net_dev(struct netstats*);


#endif 


