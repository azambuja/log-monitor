@echo off

set INC=..\..\idl

rem ---- Events ----
echo Compilando events.idl
idlj -fallTIE -I %INC% %INC%\events.idl

rem ---- deployment ----
echo Compilando deployment.idl
idlj -fallTIE -I %INC% %INC%\deployment.idl

rem ---- scs ----
echo Compilando scs.idl
idlj -fallTIE -I %INC% %INC%\scs.idl

rem ---- SCS.Instrumentation ----
echo Compilando scs.instrumentation.idl
idlj -fallTIE -I %INC% %INC%\scs.instrumentation.idl

rem ---- SCS.Instrumentation ----
echo Compilando reasoning.idl
idlj -fallTIE -I %INC% %INC%\reasoning.idl

rem ---- SCS.Demos ----
echo Compilando demos
idlj -fallTIE -I %INC% %INC%\pingPong.idl
idlj -fallTIE -I %INC% %INC%\mapReduce.idl
idlj -fallTIE -I %INC% %INC%\philosopher.idl

rem ---- SCS.Instrumentation.test ----
rem idlj -fallTIE scs.instrumentation.test.idl


