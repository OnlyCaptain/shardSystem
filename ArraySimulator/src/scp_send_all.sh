#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"47.103.219.188"
)
port=22
target=/root/
sendf=../../release/SingleShard.jar
for ip in ${ips[*]}
do
	/usr/bin/expect<<-EOF   #shell中使用expect
	set timeout 90
	spawn scp -r -P $port $sendf $user@$ip:$target
	expect "password"
	send "$pwd\r"
    expect "$sendf"
	exit
	interact
	expect eof
	EOF
done
