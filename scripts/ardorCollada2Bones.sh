#!/bin/bash

DIR=`dirname $BASH_SOURCE`/..

CP=$DIR/build/bones.jar
CP=$CP:$DIR/lib/jpct-1.26.jar

CP=$CP:$DIR/lib/ardor-core-0.7-beta.jar
CP=$CP:$DIR/lib/ardor-collada-0.7-beta.jar
CP=$CP:$DIR/lib/ardor-animation-0.7-beta.jar
CP=$CP:$DIR/lib/jaxen.jar
CP=$CP:$DIR/lib/jdom.jar
CP=$CP:$DIR/lib/google-collect-1.0-rc1.jar

java -cp $CP raft.jpct.bones.util.ArdorColladaImporter "$@"
