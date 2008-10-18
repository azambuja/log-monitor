@echo off

rem ---Este script é usado para remover os binarios do SCS numa plataforma unix.
rem ---Para executa-lo va para o diretorio src/java e digite o seguinte comando:
rem ../../scripts/build/run-make-clean.sh

echo Apagando arquivos .jar
del ..\..\package\*.jar

echo Apagando os arquivos binarios
del system\*.class
del scs\core\*.java
del scs\core\*.class
del scs\core\servant\*.class

del scs\event_service\*.java
del scs\event_service\*.class
del scs\event_service\servant\*.class

del scs\execution_node\*.java
del scs\execution_node\*.class
del scs\execution_node\servant\*.class

del scs\instrumentation\*.java
del scs\instrumentation\*.class
del scs\instrumentation\app\*.class
del scs\instrumentation\servant\*.class
del scs\instrumentation\interceptor\*.class

del scs\reasoning\*.java
del scs\reasoning\*.class
del scs\reasoning\servant\*.class
del scs\reasoning\app\*.class

del scs\container\*.java
del scs\container\*.class
del scs\container\servant\*.class

del scs\demos\pingpong\*.java
del scs\demos\pingpong\*.class
del scs\demos\pingpong\servant\*.class

del scs\demos\mapreduce\*.java
del scs\demos\mapreduce\*.class
del scs\demos\mapreduce\schedule\*.class
del scs\demos\mapreduce\servant\*.class
del scs\demos\mapreduce\app\*.class

rem del scs\demos\philosopher\*.java
rem del scs\demos\philosopher\*.class
rem del scs\demos\philosopher\servant\*.class

