#!/bin/sh


INC=../../idl

echo 'Compilando events.idl'
# ---- Events ----
idlj -fallTIE -I $INC $INC/events.idl

echo 'Compilando deployment.idl'
# ---- deployment ----
idlj -fallTIE -I $INC $INC/deployment.idl

echo 'Compilando scs.idl'
# ---- scs ----
idlj -fallTIE -I $INC $INC/scs.idl

echo 'Compilando scs.instrumentation.idl'
# ---- SCS.Instrumentation ----
idlj -fallTIE -I $INC $INC/scs.instrumentation.idl

# ---- SCS.Reasoning ----
echo Compilando reasoning.idl
idlj -fallTIE -I $INC $INC/reasoning.idl

echo 'Compilando demos'
# ---- SCS.Demos ----
idlj -fallTIE -I $INC $INC/pingPong.idl
idlj -fallTIE -I $INC $INC/mapReduce.idl
idlj -fallTIE -I $INC $INC/philosopher.idl
idlj -fallTIE -I $INC $INC/logMonitor.idl

# ---- SCS.Instrumentation.test ----
# idlj -fallTIE -I $INC $INC/scs.instrumentation.test.idl



