#!/bin/bash

# ---Este script é usado para remover os binarios do SCS numa plataforma unix.
# ---Para executa-lo va para o diretorio src/java e digite o seguinte comando:
# ../../scripts/build/run-make-clean.sh


echo Apagando arquivos .jar
\rm ../../package/*.jar

echo Apagando os arquivos binarios
rm system/*.class
rm scs/core/*.java
rm scs/core/*.class
rm scs/core/servant/*.class

rm scs/event_service/*.java
rm scs/event_service/*.class
rm scs/event_service/servant/*.class

rm scs/execution_node/*.java
rm scs/execution_node/*.class
rm scs/execution_node/servant/*.class

rm scs/instrumentation/*.java
rm scs/instrumentation/*.class
rm scs/instrumentation/app/*.class
rm scs/instrumentation/servant/*.class
rm scs/instrumentation/interceptor/*.class

rm scs/reasoning/*.java
rm scs/reasoning/*.class
rm scs/reasoning/servant/*.class
rm scs/reasoning/app/*.class

rm scs/container/*.java
rm scs/container/*.class
rm scs/container/servant/*.class

rm scs/demos/pingpong/*.java
rm scs/demos/pingpong/*.class
rm scs/demos/pingpong/servant/*.class

rm scs/demos/logmonitor/*.java
rm scs/demos/logmonitor/*.class
rm scs/demos/logmonitor/servant/*.class

rm scs/demos/mapreduce/*.java
rm scs/demos/mapreduce/*.class
rm scs/demos/mapreduce/schedule/*.class
rm scs/demos/mapreduce/servant/*.class
rm scs/demos/mapreduce/app/*.class

# rm scs/demos/philosopher/*.java
# rm scs/demos/philosopher/*.class
# rm scs/demos/philosopher/servant/*.class

