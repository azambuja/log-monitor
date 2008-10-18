@echo off

set mypath=%~dp0

pushd %mypath%..\bin

java scs.execution_node.servant.ExecutionNodeApp ..\..\scripts\execute\scs.properties

popd

