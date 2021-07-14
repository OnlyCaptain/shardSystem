#!/bin/sh
dbfile=$1
user="root"
pwd="NOR4myself!"
ips=(
"121.196.214.104"
)
port=22
target=/root/workspace/collector/collector-sqlite.db
recvf=./
for ip in ${ips[*]}
do
	/usr/bin/expect<<-EOF   #shell中使用expect
	set timeout 90
	spawn scp -r -P $port $user@$ip:$target $recvf
	expect {
        "yes/no" {send "yes\r"}
        "password" {send "$pwd\r"}
    }
	expect {
        "yes/no" {send "yes\r"}
        "password" {send "$pwd\r"}
    }
    expect "$sendf"
	interact
	expect eof
	EOF
done

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