package scs.instrumentation;


/**
* scs/instrumentation/ContainerStats.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../../idl/scs.instrumentation.idl
* Saturday, October 18, 2008 4:06:48 PM BRST
*/

public final class ContainerStats implements org.omg.CORBA.portable.IDLEntity
{
  public String containerName = null;
  public long cpuTime = (long)0;
  public double cpuUsage = (double)0;
  public double avgCpuUsage = (double)0;
  public long elapsedTime = (long)0;
  public long memoryUsage = (long)0;

  public ContainerStats ()
  {
  } // ctor

  public ContainerStats (String _containerName, long _cpuTime, double _cpuUsage, double _avgCpuUsage, long _elapsedTime, long _memoryUsage)
  {
    containerName = _containerName;
    cpuTime = _cpuTime;
    cpuUsage = _cpuUsage;
    avgCpuUsage = _avgCpuUsage;
    elapsedTime = _elapsedTime;
    memoryUsage = _memoryUsage;
  } // ctor

} // class ContainerStats