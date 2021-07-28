#!/bin/sh
dbfile=$1
user="root"
pwd="NOR4myself!"
ips=(
"120.78.190.223" "119.23.64.121" "120.77.170.151"
)
port=22
target="/root/shardSimulator/workspace/shard_*/*-sqlite.db"
recvf=./database/
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
