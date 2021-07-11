#!/usr/bin/expect           
set user "root"           
set pwd "NOR4myself!"
set host "47.103.219.188"

set timeout -1              
spawn scp -r -P 22 ./config-dev.json $user@$host:/root/
expect {                  
    "*yes/no" {send "yes\r";exp_continue}
    "*password:" {send "$pwd\r"}
}
expect "]*"                
send "touch /home/hadoop/aa.txt\r"
expect "]*"
send "echo hello world >> /home/hadoop/aa.txt\r"
expect "]*"
[interact]               
send "exit\r"           
