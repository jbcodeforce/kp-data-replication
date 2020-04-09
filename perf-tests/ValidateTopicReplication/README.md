# Validate the topic replication

The tests in this folder validate the 10 partition topic to 5 partition topic replication.

## Pre-requisite

* Create a topic with 10 partitions on source cluster (e.g. shipments)
* Create a topic with 5 partitions on target cluster (e.g. source.shipments)
* Create a mirror maker 2 configuration to replicate the topic
* Send 2000 records to the source topic using the producer code []()
* Start the consumer on target topic in a unique consumer group so it gets messages from all the topic, compare the number of message received.

## Run

* export the environment variable to access source cluster:

```shell
export KAFKA_BROKERS=broker-3-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-1-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-0-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-5-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-2-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-4-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093
export KAFKA_APIKEY="event stream apikey"
```

The following commands can be run withing a bash using a remote connection to a kafka broker using a command like: ` oc exec -ti eda-demo-24-cluster-kafka-2  bash`

* Get list of topic on the target cluster

```
./kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --list
```

* Create target topic:

`./kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --create --partitions=5 --topic source.accounts`

* Get topic information

```shell
./kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --describe --topic source.accounts

Topic: source.accounts	PartitionCount: 5	ReplicationFactor: 3	Configs: message.format.version=2.4-IV1
	Topic: source.accounts	Partition: 0	Leader: 0	Replicas: 0,2,1	Isr: 0,2,1
	Topic: source.accounts	Partition: 1	Leader: 2	Replicas: 2,1,0	Isr: 2,1,0
	Topic: source.accounts	Partition: 2	Leader: 1	Replicas: 1,0,2	Isr: 1,0,2
	Topic: source.accounts	Partition: 3	Leader: 0	Replicas: 0,1,2	Isr: 0,1,2
	Topic: source.accounts	Partition: 4	Leader: 2	Replicas: 2,0,1	Isr: 2,0,1
```


* Start consumer

Export the environment variables to point to the target cluster:

```shell
export KAFKA_CERT=/home/ca.crt
export KAFKA_TGT_BROKERS=eda-demo-24-cluster-kafka-bootstrap-eda-strimzi-kafka24.gse-eda-demo-43-fa9ee67c9ab6a7791435450358e564cc-0000.us-east.containers.appdomain.cloud
```

Start the consumer to run locally but remote connected to the kafka cluster:

```shell
docker run -ti -v $(pwd):/home -e KAFKA_BROKERS=$KAFKA_TGT_BROKERS -e KAFKA_CERT=/home ibmcase/python37  bash -c "python /home/PerfConsumer.py --topic source.accounts"
```

Or run the consumer inside openshift

```
oc run kafka-consumer -ti --image=strimzi/kafka:latest-kafka-2.4.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --topic source.accounts -from-beginning
```

* Start producer

```shell
docker run -ti -v $(pwd):/home  -e KAFKA_BROKERS=$KAFKA_SRC_BROKERS -e KAFKA_APIKEY=$KAFKA_APIKEY ibmcase/python37 bash -c "python ProducerPerformance.py --file /home/ValidateTopicReplication/testplayload.json --size 500 --keyname identifier --topic accounts"
```

The number of records on the consumer is 500, read from 5 partitions.
