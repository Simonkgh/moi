To set up a VM with zookeeper (1 node) + kafka (1 node) + kafka-connect in distributed mode (2 nodes):

* install virtualbox
* install vagrant
* git checkout this directory (presumably you've already done that :-)
* sh setup.sh  (downloads Kafka to local directory)
* vagrant up

Then login to the VM is possible via "vagrant ssh" (and then use sudo for root rights)

Kafka-connect is available at address 192.168.33.10 ports 9091 and 9092, eg:
 # get list of all configured connectors
 curl -XGET http://192.168.33.10:9091/connectors

Within the VM, the following commands may be useful:

/opt/kafka/current/bin/kafka-topics.sh --zookeeper localhost:9099 --list
/opt/kafka/current/bin/kafka-topics.sh --zookeeper localhost:9099 --create --replication-factor 1 --partitions 2 --topic netmax-raw
/opt/kafka/current/bin/kafka-console-consumer.sh --bootstrap-server localhost:9095 --topic netmax-raw --property print.key=true --from-beginning
