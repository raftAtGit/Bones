echo off
set DIR=%~dp0\..

set CP=%DIR%\build\bones.jar
set CP=%CP%;%DIR%\lib\jpct-1.26.jar

set CP=%CP%;%DIR%\lib\ardor-core-0.7-beta.jar
set CP=%CP%;%DIR%\lib\ardor-collada-0.7-beta.jar
set CP=%CP%;%DIR%\lib\ardor-animation-0.7-beta.jar
set CP=%CP%;%DIR%\lib\jaxen.jar
set CP=%CP%;%DIR%\lib\jdom.jar
set CP=%CP%;%DIR%\lib\google-collect-1.0-rc1.jar

java -cp %CP% raft.jpct.bones.util.ArdorColladaImporter %1 %2 %3 %4 %5 %6 %7 %8 %9
