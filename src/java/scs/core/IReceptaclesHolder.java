package scs.core;

/**
* scs/core/IReceptaclesHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../../idl/scs.idl
* Saturday, October 18, 2008 4:06:47 PM BRST
*/

public final class IReceptaclesHolder implements org.omg.CORBA.portable.Streamable
{
  public scs.core.IReceptacles value = null;

  public IReceptaclesHolder ()
  {
  }

  public IReceptaclesHolder (scs.core.IReceptacles initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = scs.core.IReceptaclesHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    scs.core.IReceptaclesHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return scs.core.IReceptaclesHelper.type ();
  }

}