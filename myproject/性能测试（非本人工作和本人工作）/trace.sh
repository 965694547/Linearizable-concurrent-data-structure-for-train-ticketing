#!/bin/sh
rm ticketingsystem/*.class
javac -encoding UTF-8 -cp . ticketingsystem/Trace.java #编译命令
java -cp . ticketingsystem/Trace #执行命令
