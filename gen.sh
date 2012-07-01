#!/bin/sh

#path to stringtemplate-3 jar
ST=/usr/share/stringtemplate/lib/stringtemlate.jar

java -cp $ST:target/classes name.kazennikov.dafsa.StGen long.xml TroveFSA.st src/main/java/name/kazennikov/dafsa/LongFSA.java


