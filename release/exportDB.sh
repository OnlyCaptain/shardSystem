#!/bin/sh
path=$1
files=$(ls $path)
for filename in $files
do
  /usr/bin/expect<<-EOF   #shell中使用expect
  set timeout 90
  spawn sqlite3 $path/$filename
  expect "sqlite>"
  send ".headers on\r"
  expect "sqlite>"
  send ".mode csv\r"
  expect "sqlite>"
  send ".output outputcsv/${filename:0:7}.csv\r"
  expect "sqlite>"
  send "select * from transactions;\r"
  expect "sqlite>"
  send ".quit\r"
  expect EOF
  wait
EOF
done
