@echo off

rem Script para execucao do processo do container
rem Recebe como parametro o IOR do objeto ContainerParent que ele deve notificar quando estiver pronto

set INTERCEPTOR="-Dorg.omg.PortableInterceptor.ORBInitializerClass.scs.instrumentation.interceptor.LoggingServiceORBInitializer"

set INTERCEPTORSTATS="-Dorg.omg.PortableInterceptor.ORBInitializerClass.scs.instrumentation.interceptor.StatsServiceORBInitializer"

set LOGNAME="-DlogInterceptor.name=Container"

set LOGDIR="-DlogInterceptor.dir=logs"

set LOGDIR="-Dcontainer.loghost=localhost"


set t=%TIME%
set t=%t:,=%
set t=%t::=%
set t=%t: =%


rem set STDOUT=NUL
set STDOUT=stdout_%t%.txt

rem set STDERR=NUL
set STDERR=stderr_%t%.txt

set mypath=%~dp0
pushd %mypath%..\bin

rem ------ execucao do container com instrumentacao 
java %INTERCEPTOR% %INTERCEPTORSTATS% %LOGNAME% %LOGDIR% -classpath . scs.container.servant.ContainerApp ..\..\scripts\execute\scs.properties %1 %2 %3 >%STDOUT% 2>%STDERR%

rem ------ executa o container sem instrumentacao 
rem start java -classpath . scs.container.servant.ContainerApp ..\..\scripts\execute\scs.properties %1 %2 %3 


popd
