#!/bin/sh
dbfile=$1
user="root"
pwd="NOR4myself!"
ips=(
"120.77.210.5" "39.108.143.106" "120.78.171.208" "112.74.42.221" "119.23.58.253" "119.23.70.207" "39.108.235.214" "120.77.33.18" "39.108.96.160" "119.23.46.71" "39.108.121.21" "119.23.43.139" "119.23.255.162" "39.108.60.174" "120.77.34.146" "39.108.91.160" 
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
