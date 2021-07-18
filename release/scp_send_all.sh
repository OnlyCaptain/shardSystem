#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"112.74.34.239" "119.23.62.192" "39.108.111.140" "120.77.219.209" "39.108.37.208" "120.77.145.0" "112.74.186.10"
)
port=22
target=/root/
sendf=./shardSimulator
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
