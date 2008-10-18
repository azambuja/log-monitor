@echo off

set mypath=%~dp0
pushd %mypath%..\bin

java scs.chat.MetaTester localhost 1050

popd

pause