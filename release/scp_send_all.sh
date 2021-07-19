#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"120.77.148.237" "119.23.110.80" "39.108.129.21" "39.108.108.115" "120.77.175.243" "120.77.245.66" "39.108.131.228" "120.78.166.116" "120.77.222.156" "39.108.98.46" "119.23.48.137" "39.108.81.122" "119.23.105.89" "39.108.82.177" "39.108.183.4" "39.108.232.107" "119.23.62.192" "120.77.221.108" "120.77.151.82" "39.108.93.184" "120.78.169.8"
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
