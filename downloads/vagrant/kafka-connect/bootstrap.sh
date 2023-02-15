#!/usr/bin/env bash
#
# Ports:
#  zookeeper: 2181
#  kafka: 9092
#  kafka-connect

#-----------------------------------------
echo "provisioning" > /tmp/provisioned.txt
apt-get update

#-----------------------------------------
# Enable Debian "unattended installation" mode (see http://www.microhowto.info/howto/perform_an_unattended_installation_of_a_debian_package.html)
export DEBIAN_FRONTEND=noninteractive

# To set specific answers to install questions:
#   echo "pkgname questionid datatype answer" | debconf-set-selections
#
# To find out the necessary pgkname/questionid/answer values, make an interactive install then
#   debconf-get-selections | grep ...

#-----------------------------------------
# Ensure that cryptography operations do not stall due to lack of entropy
apt-get --yes install haveged

#-----------------------------------------
# Set up a local Kerberos instance, with users.
#
# This setup deliberately does not use the "default realm" to avoid having misconfiguration accidentally
# work by "falling back" to the default realm when that was not really intended. Instead, two realms
# with names "UMDEMO.COM" and "UMTEST.COM" are set up with users.
#
# Heimdal Kerberos is used rather than MIT Kerberos, due to problems with MIT kerberos when configuring
# multiple realms for a single KDC instance.
#
# The following steps which install kafka-broker and kafka-connect do not yet use this KDC, ie it is
# set up but not referenced by default. 

# Install heimdal kerberos servers, with automated answers via debconf-set-selections
echo "krb5-config   krb5-config/kerberos_servers    string  localhost" | debconf-set-selections
echo "krb5-config   krb5-config/default_realm       string  DEFAULT_REALM" | debconf-set-selections
apt-get --yes install heimdal-kdc heimdal-servers

cp /vagrant/config/krb5.conf /etc/krb5.conf
echo "*/admin@UMDEMO.COM all" >> /etc/heimdal-kdc/kadmind.acl
echo "*/admin@UMTEST.COM all" >> /etc/heimdal-kdc/kadmind.acl

kadmin -l init --realm-max-ticket-life=unlimited --realm-max-renewable-life=unlimited UMDEMO.COM
kadmin -l -r UMDEMO.COM add --use-defaults --password=password umdemo/admin
kadmin -l -r UMDEMO.COM add --use-defaults --password=password umdemo1
kadmin -l -r UMDEMO.COM add --use-defaults --password=password broker1
kadmin -l -r UMDEMO.COM add --use-defaults --password=password client1

kadmin -l init --realm-max-ticket-life=unlimited --realm-max-renewable-life=unlimited UMTEST.COM
kadmin -l -r UMTEST.COM add --use-defaults --password=password umtest/admin
kadmin -l -r UMTEST.COM add --use-defaults --password=password umtest1

kadmin -l list '*'

service heimdal-kdc restart
journalctl -u heimdal-kdc

#-----------------------------------------
echo "installing Java" >> /tmp/provisioned.txt
apt-get --yes install --assume-yes openjdk-8-jdk-headless

#-----------------------------------------
echo "install Kafka" >> /tmp/provisioned.txt
adduser --quiet --system --no-create-home kafka

# Make directory for logging output (might not be used)
mkdir /var/log/kafka-connect
chown -R kafka /var/log/kafka-connect

# Make directory for application code
mkdir /opt/kafka

# Copy kafka binaries and scripts
cp -r /vagrant/kafka_2.12-* /opt/kafka
rm /opt/kafka/*.tgz*
ln -s /opt/kafka/kafka_2.12-* /opt/kafka/current

# Copy kafka configuration files
cp -r /vagrant/kafka/config /opt/kafka/config

# Copy custom kafka-connect plugins
cp -r /vagrant/kafka/plugins /opt/kafka/plugins

# Set ownership
chown -R kafka /opt/kafka

# Set up systemd services for zookeeper and kafka
echo "install kafka systemd" >> /tmp/provisioned.txt
cp /vagrant/kafka/systemd/* /etc/systemd/system
systemctl daemon-reload
systemctl enable zookeeper --now
systemctl enable kafka --now

#-----------------------------------------
echo "install kafka-connect" >> /tmp/provisioned.txt

# wait for systemd to start up kafka above
sleep 10

KAFKA_BIN="/opt/kafka/current/bin"
KAFKA_TOPIC="$KAFKA_BIN/kafka-topics.sh --zookeeper localhost:9099"

# Create the internal topics needed for kafka-connect (with replication=3 and partitions=10 for production)
$KAFKA_TOPIC --create --replication-factor 1 --partitions 2 --topic connect-offsets --config cleanup.policy=compact
$KAFKA_TOPIC --create --replication-factor 1 --partitions 2 --topic connect-configs --config cleanup.policy=compact
$KAFKA_TOPIC --create --replication-factor 1 --partitions 2 --topic connect-status  --config cleanup.policy=compact

systemctl enable kafka-connect-1 --now
systemctl enable kafka-connect-2 --now

#-----------------------------------------
echo "provisioned" >> /tmp/provisioned.txt
