# 开启activemq
systemctl stop firewalld.service
source /etc/profile;
#/etc/init.d/activemq start
/opt/apache-activemq/bin/activemq stop

# 开启服务
systemctl stop firewalld.service
source /etc/profile;
cd shardSimulator
rm -rf workspace
netstat -anp | grep -E '60635|58052' | awk '{split($7,b,"/"); print b[1]}' | sed -n '1p' |xargs kill
# nohup java -jar shardSimulator.jar config.json  >output 2>&1 &
java -jar shardSimulator.jar ./config.json

# 开启collector
systemctl stop firewalld.service
source /etc/profile;
rm -rf workspace
netstat -anp | grep -E '57050' | awk '{split($7,b,"/"); print b[1]}' | sed -n '1p' |xargs kill
java -jar delayCollector.jar