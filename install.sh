# 开启activemq
systemctl stop firewalld.service
source /etc/profile;
/etc/init.d/activemq star start

# 开启服务
systemctl stop firewalld.service
source /etc/profile;
cd shardSimulator
rm -rf workspace
kill `netstat -anp | grep 60635 | awk '{split($7,b,"/"); print b[1]}' | sed -n '1p'`
kill `netstat -anp | grep 58052 | awk '{split($7,b,"/"); print b[1]}' | sed -n '1p'`
java -jar pbftSimulator.jar ./config.json

# 开启collector
systemctl stop firewalld.service
source /etc/profile;
rm -rf workspace
kill `netstat -anp | grep 57050 | awk '{split($7,b,"/"); print b[1]}' | sed -n '1p'`
java -jar delayCollector.jar