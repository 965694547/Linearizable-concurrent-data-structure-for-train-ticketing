#!/bin/sh

## compile Trace.java and put .class into bin
rm ticketingsystem/*.class
javac -encoding UTF-8 -cp . ticketingsystem/Trace_check.java #编译命令

result=1

## begin test
for i in $(seq 1 50); do 
    echo -n $i
    java -cp . ticketingsystem/Trace_check> trace 
    java -jar checker.jar --no-path-info --coach 3 --seat 5 --station 5 < trace
    if [ $? != 0 ]; then
        echo "Test failed!!! see trace file to debug"
        result=0
        break
    fi
   rm trace
done

if [ $result == 1 ]; then
    echo "Test passed!!!"
fi
