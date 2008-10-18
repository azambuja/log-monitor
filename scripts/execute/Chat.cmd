@echo off

set mypath=%~dp0
pushd %mypath%..\bin

java scs.chat.Chat

popd