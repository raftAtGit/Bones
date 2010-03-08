#!/bin/bash

DIR=`dirname $BASH_SOURCE`/..

CP=$DIR/build/bones.jar
CP=$CP:$DIR/lib/jpct-1.21.alpha.jar

CP=$CP:$DIR/lib/jme-2.0.1.jar
CP=$CP:$DIR/lib/jme-model-2.0.1.jar
CP=$CP:$DIR/lib/jme-ogrexml-2.0.1-exposed.jar

java -cp $CP raft.jpct.bones.util.JMEOgreImporter "$@"
