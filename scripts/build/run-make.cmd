@echo off

rem Este script eh usado para compilar o SCS numa plataforma windows.
rem Ele gera quatro arquivos jar (core.jar, executionNode.jar, container.jar e
rem pingPong.jar)---
rem Para executa-lo va para o diretorio src/java e digite o seguinte comando:
rem ../../scripts/build/run-make.cmd


set BUILD=..\..\scripts\build
set EXEC=..\..\scripts\execute

echo Processando os arquivos idl
call %BUILD%\run-idlj

echo Compilando system
javac system\*.java

echo Gerando core.jar
javac scs\core\*.java
javac scs\core\servant\*.java

javac scs\event_service\*.java
javac scs\event_service\servant\*.java

jar cf core.jar scs\core\*.class scs\core\servant\*.class scs\event_service\*.class scs\event_service\servant\*.class

echo Gerando executionNode.jar
javac scs\execution_node\*.java
javac scs\execution_node\servant\*.java

jar cfm executionNode.jar %BUILD%\manifestExecutionNode.txt scs\execution_node\*.class scs\execution_node\servant\*.class

echo Gerando container.jar
javac scs\instrumentation\*.java
javac scs\instrumentation\app\*.java
javac scs\instrumentation\servant\*.java
javac scs\instrumentation\interceptor\*.java

javac scs\container\*.java
javac scs\container\servant\*.java

jar cfm container.jar %BUILD%\manifestContainer.txt scs\instrumentation\*.class scs\instrumentation\app\*.class scs\instrumentation\servant\*.class scs\instrumentation\interceptor\*.class scs\container\*.class scs\container\servant\*.class

echo Gerando reasoning
javac scs\reasoning\*.java
javac scs\reasoning\servant\*.java
javac scs\reasoning\app\*.java

rem jar cfm monitor.jar %BUILD%\manifestMonitor.txt scs\reasoning\*.class scs\reasoning\servant\*.class scs\reasoning\app\DataCollectionApp.class

rem jar cfm reasoning.jar %BUILD%\manifestReasoning.txt scs\reasoning\*.class scs\reasoning\servant\*.class scs\reasoning\app\ReasoningApp.class

echo Gerando pingPong.jar
javac scs\demos\pingpong\*.java
javac scs\demos\pingpong\servant\*.java

jar cfm pingPong.jar %BUILD%\manifestPingPong.txt scs\demos\pingpong\*.class scs\demos\pingpong\servant\*.class 

echo Gerando mapReduce.jar
javac scs\demos\mapreduce\*.java
javac scs\demos\mapreduce\schedule\*.java
javac scs\demos\mapreduce\servant\*.java
javac scs\demos\mapreduce\app\*.java

rem jar cfm mapReduce.jar %BUILD%\manifestMapReduce.txt scs\demos\mapreduce\*.class scs\demos\mapreduce\servant\*.class 

rem echo Gerando philosopher.jar
rem javac scs\demos\philosopher\*.java
rem javac scs\demos\philosopher\servant\*.java

rem jar cfm philosopher.jar %BUILD%\manifestPhilosopher.txt scs\demos\philosopher\*.class scs\demos\philosopher\servant\*.class

echo Movendo jars para package
mv *.jar ..\..\package


 

