@echo off

set mypath=%~dp0
pushd %mypath%..\bin

java scs.chat.Administrator -ORBInitialHost=localhost -ORBInitialPort=1050

popd
