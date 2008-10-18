#!/bin/sh

mypath=`dirname $0`
pushd $mypath/../bin

java scs.chat.Chat

popd