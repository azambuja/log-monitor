javac system\SystemInformation.java

javah system.SystemInformation

mv *.h system\

cd system

gcc -D_JNI_IMPLEMENTATION_ -Wl,--kill-at -IC:/java/jdk1.5.0_06/include -IC:/java/jdk1.5.0_06/include/win32 -shared system_SystemInformation.c -o system_SystemInformation.dll C:/dev-cpp/lib/libpsapi.a
