package scs.execution_node;


/**
* scs/execution_node/ExecutionNodeOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from ../../idl/deployment.idl
* Saturday, October 18, 2008 4:06:48 PM BRST
*/

public interface ExecutionNodeOperations 
{
  scs.core.IComponent startContainer (String container_name, scs.execution_node.Property[] props) throws scs.execution_node.ContainerAlreadyExists, scs.execution_node.InvalidProperty;
  void stopContainer (String container_name) throws scs.core.InvalidName;
  scs.core.IComponent getContainer (String container_name);
  scs.execution_node.ContainerDescription[] getContainers ();
  String getName ();
} // interface ExecutionNodeOperations