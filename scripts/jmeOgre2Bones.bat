echo off
set DIR="\.."

set CP=%DIR%\build\bones.jar
set CP=%CP%:%DIR%\lib\jpct-1.21_beta3.jar

set CP=%CP%:%DIR%\lib\jme-2.1.jar
set CP=%CP%:%DIR%\lib\jme-model-2.1.jar
vCP=%CP%:%DIR%\lib\jme-ogrexml-2.1-exposed.jar

java -cp %CP% raft.jpct.bones.util.JMEOgreImporter "$@"