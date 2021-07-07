rm -rf *shardRelease*

wget --no-check-certificate --content-disposition https://gitee.com/OnlyCaptain/shardRelease/attach_files/742519/download/shardRelease.zip
unzip shardRelease.zip

systemctl stop firewalld.service
source /etc/profile;
cd shardRelease
rm -rf workspace
kill `netstat -anp | grep 60635 | awk '{split($7,b,"/"); print b[1]}' | sed -n '1p'`
kill `netstat -anp | grep 58052 | awk '{split($7,b,"/"); print b[1]}' | sed -n '1p'`
java -jar MultiShard.jar ./config.json

