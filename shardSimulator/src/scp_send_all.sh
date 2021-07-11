#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"8.134.61.104" "8.134.54.41" "8.134.71.43" "8.134.54.209" "8.134.56.17" "8.134.69.97" "8.134.59.230"
)
port=22
target=/root/
sendf=./SingleShard.zip
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
