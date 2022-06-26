#!/bin/sh
rm ticketingsystem/*.class
javac -encoding UTF-8 -cp . ticketingsystem/Test1.java #编译命令
java -cp . ticketingsystem/Test1 #执行命令
