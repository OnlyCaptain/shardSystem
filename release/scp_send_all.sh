#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"120.79.211.251" "120.79.139.207" "120.79.143.167" "120.79.80.8" "119.23.72.251" "47.106.101.159" "120.78.201.10" "47.106.91.222" "120.79.179.222" "120.79.158.187" "39.108.113.236" "120.77.40.164" "120.78.240.184" "120.79.193.4" "120.79.86.164" "39.108.52.161" "120.79.88.175" "47.106.95.64" "120.79.200.184" "120.79.11.50" "47.106.88.137"
)
port=22
target=/root/
sendf=./shardSimulator
Njob=10
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
