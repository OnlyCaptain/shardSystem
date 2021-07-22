#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"120.77.148.237"
)
port=22
target=/root/
sendf=./shardSimulator/workspace
Njob=10
for ip in ${ips[*]}
do
	/usr/bin/expect<<-EOF   #shell中使用expect
	set timeout 90
	spawn scp -r -P $port $user@$ip:$target/$sendf ./server-log
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
