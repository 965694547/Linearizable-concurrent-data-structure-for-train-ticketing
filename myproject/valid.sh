#!/bin/sh
rm ticketingsystem/*.class
javac -encoding UTF-8 -cp . ticketingsystem/Valid.java #编译命令
java -cp . ticketingsystem/Valid #执行命令
