#!/bin/bash

DIR=`dirname $BASH_SOURCE`/..

CP=$DIR/build/bones.jar
CP=$CP:$DIR/lib/jpct-1.26.jar

CP=$CP:$DIR/lib/jme-2.1.jar
CP=$CP:$DIR/lib/jme-model-2.1.jar
CP=$CP:$DIR/lib/jme-ogrexml-2.1.jar

java -cp $CP raft.jpct.bones.util.JMEOgreImporter "$@"
