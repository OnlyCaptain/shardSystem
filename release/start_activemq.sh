#!/bin/sh
user="root"
pwd="NOR4myself!"
ips=(
"47.107.90.15"
)
port=22
target=/root/
sendf=./shardSimulator
Njob=10
for ip in ${ips[*]}
do
	/usr/bin/expect<<-EOF   #shell中使用expect
	set timeout 90
	spawn ssh -p $port $user@$ip
	expect {
        "yes/no" {send "yes\r"}
        "password" {send "$pwd\r"}
    }
  expect "#"
	send "/etc/init.d/activemq start\r"
	expect "#"
	send "cd shardSimulator\r"
	expect "#"
	send "rm -rf workspace\r"
	expect "#"
	send -- {netstat -anp | grep -E '60635|58052' | awk '{split(\$7,b,"/"); print b[1]}' | sed -n '1p' |xargs kill }
	send -- \r
	expect "#"
	send "nohup java -jar shardSimulator.jar config.json  >output 2>&1 &\r"
	expect "#"
	send "ls\r"
	expect "#"
	send "exit\r"
	expect eof
	EOF
done
