#!/bin/sh

# Script para execucao do processo do container
# Recebe como parametro o IOR do objeto ContainerParent que ele deve notificar quando estiver pronto

INTERCEPTOR="-Dorg.omg.PortableInterceptor.ORBInitializerClass.scs.instrumentation.interceptor.LoggingServiceORBInitializer"

INTERCEPTORSTATS="-Dorg.omg.PortableInterceptor.ORBInitializerClass.scs.instrumentation.interceptor.StatsServiceORBInitializer"

PUREINTERCEPTOR="-Dorg.omg.PortableInterceptor.ORBInitializerClass.scs.instrumentation.interceptor.StatsServicePuerORBInitializer"

LOGNAME="-DlogInterceptor.name=Container"

LOGDIR="-DlogInterceptor.dir=logs"

LOGDIR="-Dcontainer.loghost=localhost"

TIME=`date +%s`

# STDOUT=NUL
STDOUT=stdout_$TIME.txt

# STDERR=NUL
STDERR=stderr_$TIME.txt

mypath=`dirname $0`
pushd $mypath/../bin

JAVA_HOME=/usr/bin

# ------ execucao do container com instrumentacao 
#export LD_LIBRARY_PATH=/home/prj/openbus/work/scs_healing/sand/lib

#/usr/java/jdk1.5.0_05/bin/java -Xmx500m $INTERCEPTORSTATS $LOGNAME $LOGDIR -classpath . scs.container.servant.ContainerApp ../../scripts/execute/scs.properties $1 $2 $3 >$STDOUT 2>$STDERR

#/usr/java/jdk1.5.0_05/bin/java -Xmx500m $INTERCEPTORPURE $LOGNAME $LOGDIR -classpath . scs.container.servant.ContainerApp ../../scripts/execute/scs.properties $1 $2 $3 >$STDOUT 2>$STDERR

# ------ executa o container sem instrumentacao 
$JAVA_HOME/java -classpath . scs.container.servant.ContainerApp ../../scripts/execute/scs.properties $1 $2 $3  >$STDOUT 2>$STDERR

popd
