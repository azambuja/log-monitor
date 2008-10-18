/* ------------------------------------------------------------------------- */
/*
 * An implementation of JNI methods in com.vladium.utils.SystemInformation
 * class. The author compiled it using Microsoft Visual C++ and GCC for Win32 but the code
 * should be easy to use with any compiler for win32 platform.
 *
 * For simplicity, this implementaion assumes JNI 1.2+ and omits error handling.
 *
 * Enhanced by Peter V. Mikhalenko (C) 2004, Deutsche Bank [peter@mikhalenko.com]
 * Original source (C) 2002, Vladimir Roubtsov [vlad@trilogy.com]
 */
/* ------------------------------------------------------------------------- */

#include <windows.h>
#include <psapi.h>
#include "system_SystemInformation.h"

static jint s_PID;
static HANDLE s_currentProcess;
static int alreadyDetached;
static int s_numberOfProcessors;
static SYSTEM_INFO systemInfo;
static WORD processorArchitecture;
static DWORD pageSize;
static DWORD processorType;
static WORD processorLevel;
static WORD processorRevision;

#define INFO_BUFFER_SIZE 32768
#define BUFSIZE 2048


/* ------------------------------------------------------------------------- */

/*
 * A helper function for converting FILETIME to a LONGLONG [safe from memory
 * alignment point of view].
 */
static LONGLONG
fileTimeToInt64 (const FILETIME * time)
{
    ULARGE_INTEGER _time;

    _time.LowPart = time->dwLowDateTime;
    _time.HighPart = time->dwHighDateTime;

    return _time.QuadPart;
}
/* ......................................................................... */

/*
 * This method was added in JNI 1.2. It is executed once before any other
 * methods are called and is ostensibly for negotiating JNI spec versions, but
 * can also be conveniently used for initializing variables that will not
 * change throughout the lifetime of this process.
 */
JNIEXPORT jint JNICALL
JNI_OnLoad (JavaVM * vm, void * reserved)
{
        
    s_PID = _getpid ();
    s_currentProcess = GetCurrentProcess ();
    // externalCPUmon = 0;
    alreadyDetached = 0;

    GetSystemInfo (& systemInfo);
    s_numberOfProcessors = systemInfo.dwNumberOfProcessors;
    processorArchitecture = systemInfo.wProcessorArchitecture;
    pageSize = systemInfo.dwPageSize;
    processorType = systemInfo.dwProcessorType;
    processorLevel = systemInfo.wProcessorLevel;
    processorRevision = systemInfo.wProcessorRevision;

    return JNI_VERSION_1_2;
}
/* ......................................................................... */

JNIEXPORT void JNICALL
JNI_OnUnload (JavaVM * vm, void * reserved)
{
        
    if (!alreadyDetached && s_currentProcess!=NULL) {
	    CloseHandle(s_currentProcess);
	    printf("[JNI Unload] Detached from native process.\n");
	    fflush(stdout);
    }
   
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
    return (jint)s_numberOfProcessors;
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
    char buf2[512];
    *buf=0;
    OSVERSIONINFOEX osvi;
    BOOL bOsVersionInfoEx;
    TCHAR  infoBuf[INFO_BUFFER_SIZE];
    DWORD  bufCharCount = INFO_BUFFER_SIZE;
    
    
    // Try calling GetVersionEx using the OSVERSIONINFOEX structure.
    // If that fails, try using the OSVERSIONINFO structure.
    ZeroMemory(&osvi, sizeof(OSVERSIONINFOEX));
    osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOEX);
    if( !(bOsVersionInfoEx = GetVersionEx ((OSVERSIONINFO *) &osvi)) )
    {
       osvi.dwOSVersionInfoSize = sizeof (OSVERSIONINFO);
       if (! GetVersionEx ( (OSVERSIONINFO *) &osvi) ) {
	    // Return empty string in case of problems
		 goto next_label;
	}
    }
   switch (osvi.dwPlatformId)
   {
      // Test for the Windows NT product family.
      case VER_PLATFORM_WIN32_NT:

         // Test for the specific product.
         if ( osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 2 )
            strcat(buf,"WinServer2003, ");

         if ( osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 1 )
            strcat(buf,"WinXP ");

         if ( osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 0 )
            strcat(buf,"Win2K ");

         if ( osvi.dwMajorVersion <= 4 )
            strcat(buf,"WinNT ");

         // Test for specific product on Windows NT 4.0 SP6 and later.
         if( bOsVersionInfoEx )
         {
            // Test for the workstation type.
            if ( osvi.wProductType == VER_NT_WORKSTATION )
            {
               if( osvi.dwMajorVersion == 4 )
                  strcat(buf,"Workstation 4.0 " );
               else if( osvi.wSuiteMask & VER_SUITE_PERSONAL )
                  strcat(buf,"Home Edition " );
               else
                  strcat(buf,"Professional " );
            }
            
            // Test for the server type.
            else if ( osvi.wProductType == VER_NT_SERVER || 
                      osvi.wProductType == VER_NT_DOMAIN_CONTROLLER )
            {
               if( osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 2 )
               {
                  if( osvi.wSuiteMask & VER_SUITE_DATACENTER )
                     strcat(buf,"Datacenter Edition " );
                  else if( osvi.wSuiteMask & VER_SUITE_ENTERPRISE )
                     strcat(buf,"Enterprise Edition " );
                  else if ( osvi.wSuiteMask == VER_SUITE_BLADE )
                     strcat(buf,"Web Edition " );
                  else
                     strcat(buf,"Standard Edition " );
               }

               else if( osvi.dwMajorVersion == 5 && osvi.dwMinorVersion == 0 )
               {
                  if( osvi.wSuiteMask & VER_SUITE_DATACENTER )
                     strcat(buf,"Datacenter Server " );
                  else if( osvi.wSuiteMask & VER_SUITE_ENTERPRISE )
                     strcat(buf,"Advanced Server " );
                  else
                     strcat(buf,"Server " );
               }

               else  // Windows NT 4.0 
               {
                  if( osvi.wSuiteMask & VER_SUITE_ENTERPRISE )
                     strcat(buf,"Server 4.0, Enterprise Edition " );
                  else
                     strcat(buf,"Server 4.0 " );
               }
            }
         }
         else  // Test for specific product on Windows NT 4.0 SP5 and earlier
         {
            HKEY hKey;
            char szProductType[BUFSIZE];
            DWORD dwBufLen=BUFSIZE;
            LONG lRet;

            lRet = RegOpenKeyEx( HKEY_LOCAL_MACHINE,
               "SYSTEM\\CurrentControlSet\\Control\\ProductOptions",
               0, KEY_QUERY_VALUE, &hKey );
            if( lRet != ERROR_SUCCESS ) {
		    goto next_label;
		}

            lRet = RegQueryValueEx( hKey, "ProductType", NULL, NULL,
               (LPBYTE) szProductType, &dwBufLen);
            if( (lRet != ERROR_SUCCESS) || (dwBufLen > BUFSIZE) ) {
		goto next_label;
	    }

            RegCloseKey( hKey );

            if ( lstrcmpi( "WINNT", szProductType) == 0 )
               strcat(buf,"Workstation " );
            if ( lstrcmpi( "LANMANNT", szProductType) == 0 )
               strcat(buf,"Server " );
            if ( lstrcmpi( "SERVERNT", szProductType) == 0 )
               strcat(buf,"Advanced Server " );

            sprintf(buf2, "%d.%d ", (int)osvi.dwMajorVersion, (int)osvi.dwMinorVersion );
	    strcat(buf,buf2);
         }

      // Display service pack (if any) and build number.

         if( osvi.dwMajorVersion == 4 && 
             lstrcmpi( osvi.szCSDVersion, "Service Pack 6" ) == 0 )
         {
            HKEY hKey;
            LONG lRet;

            // Test for SP6 versus SP6a.
            lRet = RegOpenKeyEx( HKEY_LOCAL_MACHINE,
               "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Hotfix\\Q246009",
               0, KEY_QUERY_VALUE, &hKey );
            if( lRet == ERROR_SUCCESS ) {
               sprintf(buf2, "SP 6a (Build %d), ", (int)(osvi.dwBuildNumber & 0xFFFF) );         
	       strcat(buf,buf2);
	    }
            else // Windows NT 4.0 prior to SP6a
            {
               sprintf(buf2, "%s (Build %d), ",
                  osvi.szCSDVersion,
                  (int)(osvi.dwBuildNumber & 0xFFFF));
	       strcat(buf,buf2);
            }

            RegCloseKey( hKey );
         }
         else // not Windows NT 4.0 
         {
            sprintf(buf2, "%s (Build %d), ",
               osvi.szCSDVersion,
               (int)(osvi.dwBuildNumber & 0xFFFF));
	    strcat(buf,buf2);
         }


         break;

      // Test for the Windows Me/98/95.
      case VER_PLATFORM_WIN32_WINDOWS:

         if (osvi.dwMajorVersion == 4 && osvi.dwMinorVersion == 0)
         {
             strcat(buf,"Win95 ");
             if ( osvi.szCSDVersion[1] == 'C' || osvi.szCSDVersion[1] == 'B' )
                strcat(buf,"OSR2 " );
         } 

         if (osvi.dwMajorVersion == 4 && osvi.dwMinorVersion == 10)
         {
             strcat(buf,"Win98 ");
             if ( osvi.szCSDVersion[1] == 'A' )
                strcat(buf,"SE " );
         } 

         if (osvi.dwMajorVersion == 4 && osvi.dwMinorVersion == 90)
         {
             strcat(buf,"WinME ");
         } 
         break;

      case VER_PLATFORM_WIN32s:

         strcat(buf,"Win32s ");
         break;
   }
     
next_label:

   strcat(buf,"\r\n         on ");
   // Get and display the name of the computer. 
   bufCharCount = INFO_BUFFER_SIZE;
   if( !GetComputerName( infoBuf, &bufCharCount ) )
    	goto next_label_2; 
   strcat(buf, infoBuf ); 
    
next_label_2:
    strcat(buf," (");
    if (!(osvi.dwPlatformId==VER_PLATFORM_WIN32_WINDOWS && osvi.dwMajorVersion == 4 && osvi.dwMinorVersion == 0)) {
	    // Win95 does not keep CPU info in registry
	    LONG lRet;
	    HKEY hKey;
            char szOrigCPUType[BUFSIZE];
	    int i=0;
            DWORD dwBufLen=BUFSIZE;
            lRet = RegOpenKeyEx( HKEY_LOCAL_MACHINE,
               "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0",
               0, KEY_QUERY_VALUE, &hKey );
            if( lRet != ERROR_SUCCESS ) {
		    goto next_label_3;
		}

            lRet = RegQueryValueEx( hKey, "ProcessorNameString", NULL, NULL,
               (LPBYTE) szOrigCPUType, &dwBufLen);
            if( (lRet != ERROR_SUCCESS) || (dwBufLen > BUFSIZE) ) {
		goto next_label_3;
	    }
            RegCloseKey( hKey );
	    if (strlen(szOrigCPUType)>0) {
		while(szOrigCPUType[i]==' ' && szOrigCPUType[i]!=0) i++;
		strcat(buf,szOrigCPUType+i);
	    } else goto next_label_3;
    } else {
next_label_3:
	    if (processorArchitecture==PROCESSOR_ARCHITECTURE_UNKNOWN) strcat(buf,"unknown_arch");
	    else if (processorArchitecture==PROCESSOR_ARCHITECTURE_INTEL) {
		    strcat(buf,"Intel ");
		    sprintf(buf2,"level %d ",processorLevel);
		    strcat(buf,buf2);
	    } else if (processorArchitecture==PROCESSOR_ARCHITECTURE_IA64) strcat(buf,"IA64 ");
	    else if (processorArchitecture==PROCESSOR_ARCHITECTURE_MIPS) strcat(buf,"MIPS ");
	    else if (processorArchitecture==PROCESSOR_ARCHITECTURE_ALPHA) strcat(buf,"Alpha ");
	    else if (processorArchitecture==PROCESSOR_ARCHITECTURE_PPC) strcat(buf,"PowerPC ");
	    else if (processorArchitecture==PROCESSOR_ARCHITECTURE_SHX) strcat(buf,"SHX ");
	    else if (processorArchitecture==PROCESSOR_ARCHITECTURE_ALPHA64) strcat(buf,"Alpha64 ");
	    else strcat(buf,"unknown_arch ");
    }
    
    strcat(buf,")");

    jstring retval = (*env)->NewStringUTF(env,buf);
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
    DWORD errCode;
    LPVOID lpMsgBuf;
    s_PID = pid;
    s_currentProcess = OpenProcess(PROCESS_ALL_ACCESS,FALSE,pid);
    if (s_currentProcess==NULL) {
	    errCode = GetLastError();
	    FormatMessage(
              FORMAT_MESSAGE_ALLOCATE_BUFFER | 
              FORMAT_MESSAGE_FROM_SYSTEM,
              NULL,
              errCode,
              MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
              (LPTSTR) &lpMsgBuf,
              0, NULL );
	    
	    printf("[CPUmon] Could not attach to native process.\n  Error code: %ld\n  Error description: %s\n",errCode,lpMsgBuf);
	    fflush(stdout);
	    LocalFree(lpMsgBuf);
	    return errCode;
    }
    printf("[CPUmon] Attached to native process.\n");
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
    if (!alreadyDetached && s_currentProcess!=NULL) {
	    CloseHandle(s_currentProcess);
	    alreadyDetached = 1;
	    printf("[CPUmon] Detached from native process.\n");
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
    FILETIME creationTime, exitTime, kernelTime, userTime;
    DWORD errCode;
    LPVOID lpMsgBuf;
    
    BOOL resultSuccessful = GetProcessTimes (s_currentProcess, & creationTime, & exitTime, & kernelTime, & userTime);
    if (!resultSuccessful) {
	    errCode = GetLastError();
	    FormatMessage(
              FORMAT_MESSAGE_ALLOCATE_BUFFER | 
              FORMAT_MESSAGE_FROM_SYSTEM,
              NULL,
              errCode,
              MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
              (LPTSTR) &lpMsgBuf,
              0, NULL );
	    
	    printf("[CPUmon] An error occured while trying to get CPU time.\n  Error code: %ld\n  Error description: %s\n",errCode,lpMsgBuf);

	    fflush(stdout);
	    LocalFree(lpMsgBuf);
	    return -1;
    }

    return (jlong) ((fileTimeToInt64 (& kernelTime) + fileTimeToInt64 (& userTime)) /
        (s_numberOfProcessors * 10000));
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
	MEMORYSTATUS stat;
	GlobalMemoryStatus (&stat);
        return (jlong)(stat.dwTotalPhys/1024);
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
        MEMORYSTATUS stat;
	GlobalMemoryStatus (&stat);
        return (jlong)(stat.dwAvailPhys/1024);
}
/* ......................................................................... */


/* define min elapsed time (in units of 10E-7 sec): */
#define MIN_ELAPSED_TIME (10000)

/*
 * Class:     system_SystemInformation
 * Method:    getProcessCPUUsage
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_system_SystemInformation_getProcessCPUUsage (JNIEnv * env, jclass cls)
{
    FILETIME creationTime, exitTime, kernelTime, userTime, nowTime;   
    LONGLONG elapsedTime;
    DWORD errCode;
    LPVOID lpMsgBuf;
    
    BOOL resultSuccessful = GetProcessTimes (s_currentProcess, & creationTime, & exitTime, & kernelTime, & userTime);
    if (!resultSuccessful) {
	    errCode = GetLastError();
	    FormatMessage(
              FORMAT_MESSAGE_ALLOCATE_BUFFER | 
              FORMAT_MESSAGE_FROM_SYSTEM,
              NULL,
              errCode,
              MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
              (LPTSTR) &lpMsgBuf,
              0, NULL );
	    
	    printf("[CPUmon] An error occured while trying to get CPU time.\n  Error code: %ld\n  Error description: %s\n",errCode,lpMsgBuf);
	    fflush(stdout);
	    LocalFree(lpMsgBuf);
	    return -1.0;
    }
    GetSystemTimeAsFileTime (& nowTime);

    /*
        NOTE: win32 system time is not very precise [~10ms resolution], use
        sufficiently long sampling intervals if you make use of this method.
    */
    
    elapsedTime = fileTimeToInt64 (& nowTime) - fileTimeToInt64 (& creationTime);
    
    if (elapsedTime < MIN_ELAPSED_TIME)
        return 0.0;
    else  
        return ((jdouble) (fileTimeToInt64 (& kernelTime) + fileTimeToInt64 (& userTime))) /
            (s_numberOfProcessors * elapsedTime);
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
    PROCESS_MEMORY_COUNTERS pmc;
    
    if ( GetProcessMemoryInfo( s_currentProcess, &pmc, sizeof(pmc)) )
    {
	return (jlong)(pmc.PagefileUsage/1024);
    } else {
	return (jlong)(0);
    }
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
    PROCESS_MEMORY_COUNTERS pmc;
    
    if ( GetProcessMemoryInfo( s_currentProcess, &pmc, sizeof(pmc)) )
    {
	return (jlong)(pmc.WorkingSetSize/1024);
    } else {
	return (jlong)(0);
    }
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
     return NULL;
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
   return NULL;      
}

#undef MIN_ELAPSED_TIME

/* ------------------------------------------------------------------------- */
/* end of file */


