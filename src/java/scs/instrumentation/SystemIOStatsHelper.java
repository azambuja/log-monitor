package scs.instrumentation;


/**
* scs/instrumentation/SystemIOStatsHelper.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../../idl/scs.instrumentation.idl
* Saturday, October 18, 2008 4:06:48 PM BRST
*/

abstract public class SystemIOStatsHelper
{
  private static String  _id = "IDL:scs/instrumentation/SystemIOStats:1.0";

  public static void insert (org.omg.CORBA.Any a, scs.instrumentation.SystemIOStats that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static scs.instrumentation.SystemIOStats extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  private static boolean __active = false;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      synchronized (org.omg.CORBA.TypeCode.class)
      {
        if (__typeCode == null)
        {
          if (__active)
          {
            return org.omg.CORBA.ORB.init().create_recursive_tc ( _id );
          }
          __active = true;
          org.omg.CORBA.StructMember[] _members0 = new org.omg.CORBA.StructMember [6];
          org.omg.CORBA.TypeCode _tcOf_members0 = null;
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ulonglong);
          _members0[0] = new org.omg.CORBA.StructMember (
            "sectorsRead",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ulonglong);
          _members0[1] = new org.omg.CORBA.StructMember (
            "timeReading",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ulonglong);
          _members0[2] = new org.omg.CORBA.StructMember (
            "sectorsWriten",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ulonglong);
          _members0[3] = new org.omg.CORBA.StructMember (
            "timeWriting",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ulonglong);
          _members0[4] = new org.omg.CORBA.StructMember (
            "nfsSectorsRead",
            _tcOf_members0,
            null);
          _tcOf_members0 = org.omg.CORBA.ORB.init ().get_primitive_tc (org.omg.CORBA.TCKind.tk_ulonglong);
          _members0[5] = new org.omg.CORBA.StructMember (
            "nfsSectorsWriten",
            _tcOf_members0,
            null);
          __typeCode = org.omg.CORBA.ORB.init ().create_struct_tc (scs.instrumentation.SystemIOStatsHelper.id (), "SystemIOStats", _members0);
          __active = false;
        }
      }
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static scs.instrumentation.SystemIOStats read (org.omg.CORBA.portable.InputStream istream)
  {
    scs.instrumentation.SystemIOStats value = new scs.instrumentation.SystemIOStats ();
    value.sectorsRead = istream.read_ulonglong ();
    value.timeReading = istream.read_ulonglong ();
    value.sectorsWriten = istream.read_ulonglong ();
    value.timeWriting = istream.read_ulonglong ();
    value.nfsSectorsRead = istream.read_ulonglong ();
    value.nfsSectorsWriten = istream.read_ulonglong ();
    return value;
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, scs.instrumentation.SystemIOStats value)
  {
    ostream.write_ulonglong (value.sectorsRead);
    ostream.write_ulonglong (value.timeReading);
    ostream.write_ulonglong (value.sectorsWriten);
    ostream.write_ulonglong (value.timeWriting);
    ostream.write_ulonglong (value.nfsSectorsRead);
    ostream.write_ulonglong (value.nfsSectorsWriten);
  }

}