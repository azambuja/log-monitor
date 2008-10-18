#!/bin/bash

# ---Este script é usado para compilar o SCS numa plataforma unix.
# ---Ele gera quatro arquivos jar (core.jar, executionNode.jar, container.jar e
# pingPong.jar)---
# ---Para executa-lo va para o diretorio src/java e digite o seguinte comando:
# ../../scripts/build/run-make.sh


BUILD=../../scripts/build
EXEC=../../scripts/execute

export LD_LIBRARY_PATH=/home/sand/fat32/scs_healing/versao-0.5/lib

echo Processando os arquivos idl
$BUILD/run-idlj.sh

echo Compilando system
javac -nowarn system/*.java

echo Gerando core.jar
javac -nowarn scs/core/*.java
javac -nowarn scs/core/servant/*.java

javac -nowarn scs/event_service/*.java
javac -nowarn scs/event_service/servant/*.java

jar cf core.jar scs/core/*.class scs/core/servant/*.class scs/event_service/*.class scs/event_service/servant/*.class

echo Gerando executionNode.jar
javac -nowarn scs/execution_node/*.java
javac -nowarn scs/execution_node/servant/*.java

jar cfm executionNode.jar $BUILD/manifestExecutionNode.txt scs/execution_node/*.class scs/execution_node/servant/*.class

echo Gerando container.jar
javac -nowarn scs/instrumentation/*.java
javac -nowarn scs/instrumentation/app/*.java
javac -nowarn scs/instrumentation/servant/*.java
javac -nowarn scs/instrumentation/interceptor/*.java

javac -nowarn scs/container/*.java
javac -nowarn scs/container/servant/*.java

jar cfm container.jar $BUILD/manifestContainer.txt scs/instrumentation/*.class scs/instrumentation/app/*.class scs/instrumentation/servant/*.class scs/instrumentation/interceptor/*.class scs/container/*.class scs/container/servant/*.class

echo Gerando reasoning
javac  -nowarn scs/reasoning/*.java
javac  -nowarn scs/reasoning/servant/*.java
javac  -nowarn scs/reasoning/app/*.java

echo Gerando pingPong.jar
javac -nowarn scs/demos/pingpong/*.java
javac -nowarn scs/demos/pingpong/servant/*.java

jar cfm pingPong.jar $BUILD/manifestPingPong.txt scs/demos/pingpong/*.class scs/demos/pingpong/servant/*.class 

echo Gerando logMonitor.jar
javac -nowarn scs/demos/logmonitor/*.java
javac -nowarn scs/demos/logmonitor/servant/*.java

jar cfm logMonitor.jar $BUILD/manifestLogMonitor.txt scs/demos/logmonitor/*.class scs/demos/logmonitor/servant/*.class 

echo Gerando mapReduce.jar
javac  -nowarn scs/demos/mapreduce/*.java
javac  -nowarn scs/demos/mapreduce/schedule/*.java
javac  -nowarn scs/demos/mapreduce/servant/*.java
javac  -nowarn scs/demos/mapreduce/app/*.java

#echo Gerando philosopher.jar
#javac -nowarn scs/demos/philosopher/*.java
#javac -nowarn scs/demos/philosopher/servant/*.java

#jar cfm philosopher.jar $BUILD/manifestPhilosopher.txt scs/demos/philosopher/*.class scs/demos/philosopher/servant/*.class
 
echo Movendo jars para package
mv *.jar ../../package

