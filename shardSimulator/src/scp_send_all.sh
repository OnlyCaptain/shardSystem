#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"121.43.166.235" "121.43.186.164" "121.196.200.165" "116.62.64.100" "121.196.207.72" "121.43.176.233"
)
port=22
target=/root/
sendf=./SingleShard
for ip in ${ips[*]}
do
	/usr/bin/expect<<-EOF   #shell中使用expect
	set timeout 90
	spawn scp -r -P $port $sendf $user@$ip:$target
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
