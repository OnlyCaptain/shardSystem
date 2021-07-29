#!/bin/sh
dbfile=$1
user="root"
pwd="NOR4myself!"
ips=(
"47.106.169.130" "47.106.203.100" "47.106.191.65" "120.79.129.137" "47.106.167.80" "39.108.111.231" "47.106.73.62" "47.106.104.251" "47.106.183.146" "47.106.117.225" "47.106.10.253" "47.106.12.4" "47.106.113.7" "120.78.190.223" "119.23.64.121" "120.77.170.151"
)
port=22
target="/root/shardSimulator/workspace/shard_*/*-sqlite.db"
recvf=./database/
for ip in ${ips[*]}
do
	/usr/bin/expect<<-EOF   #shell中使用expect
	set timeout 3000
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
