#!/bin/sh
# no diretorio sand
export LD_LIBRARY_PATH=/home/prj/openbus/work/scs_healing/sand/lib

echo "compilando programa java"
javac system/SystemInformation.java

echo "gerando .h"
javah -jni system.SystemInformation

echo "movendo .h"	
mv *.h system/

cd system

echo "gerando bib. compartilhada"
gcc pidstat.c system_SystemInformation_linux.c -o ../../../lib/libsystem_SystemInformation.so -shared -I ../../../../include -I ../../../../include/linux 
##gcc pidstat.c system_SystemInformation_linux.c -o ../../../lib/libsystem_SystemInformation.so -shared -I/usr/lib/jvm/java-6-sun-1.6.0.03/include -I/usr/lib/jvm/java-6-sun-1.6.0.03/include/linux
