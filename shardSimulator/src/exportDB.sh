#!/bin/sh
dbfile=$1
/usr/bin/expect<<-EOF   #shell中使用expect
set timeout 90
spawn sqlite3 $dbfile
expect "sqlite>"
send ".headers on\r"
expect "sqlite>"
send ".mode csv\r"
expect "sqlite>"
send ".output data.csv\r"
expect "sqlite>"
send "select * from transactionsTime;\r"
expect "sqlite>"
send ".quit\r"
expect eof
EOF