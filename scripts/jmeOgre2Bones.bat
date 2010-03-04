echo off
set DIR=%~dp0\..

set CP=%DIR%\build\bones.jar
set CP=%CP%;%DIR%\lib\jpct-1.21_beta3.jar

set CP=%CP%;%DIR%\lib\jme-2.0.1.jar
set CP=%CP%;%DIR%\lib\jme-model-2.0.1.jar
set CP=%CP%;%DIR%\lib\jme-ogrexml-2.0.1-exposed.jar

java -cp %CP% raft.jpct.bones.util.JMEOgreImporter %1 %2 %3 %4 %5 %6 %7 %8 %9
