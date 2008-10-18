#!/bin/sh

#mypath=`dirname $0`
#pushd $mypath/../bin

JAVA_HOME=/usr/bin/
$JAVA_HOME/java -jar ../../package/executionNode.jar ../../scripts/execute/scs.properties &

#popd

