# From Local cluster to Event Streams

## Scenario 1: From Kafka local as source to Event Streams on Cloud as Target

The test scenario goal is to send the product definitions in the local `products` topic and then start mirror maker to see the data replicated to the `source.products` topic in Event Streams cluster.

![Local Kafka to Event Streams](images/local-to-es.png)

* Set the environment variables in `setenv.sh` script for the source broker to be your local cluster, and the target to be event streams. Be sure to also set Event Streams APIKEY:

```shell
export KAFKA_SOURCE_BROKERS=kafka1:9092,kafka2:9093,kafka3:9094

export KAFKA_TARGET_BROKERS=broker-3-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-1-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-0-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-5-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-2-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-4-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093
export KAFKA_TARGET_APIKEY="<password attribut in event streams credentials>"
```

* It may be needed to create the topics in the target cluster. This depends if mirror maker 2.0 is able to access the AdminClient API. When defining APIKEy in Event streams you can have an admin, write or read access. So for Mirror Maker to create topics automatically it needs admin role.

* Send some products data to this topic. For that we use a docker python image. The docker file to build this image is `python-kafka/Dockerfile-python` so the command to build this image (if you change the image name be sure to use the new name in future command) is: `docker build -f Dockerfile-python -t jbcodeforce/python37 .`

  Once the image is built, start the python environment with the following commands:
  
  ```shell
  source ./setenv.sh
  docker run -ti -v $(pwd):/home --rm -e KAFKA_BROKERS=$KAFKA_SOURCE_BROKERS --network kafkanet jbcodeforce/python37   bash
  ```

  In this isolated python container bash shell, do the following command to send the first five products:

  ```shell
  $ echo $KAFKA_BROKERS
  kafka1:9092,kafka2:9093,kafka3:9094
  $ python SendProductToKafka.py ./data/products.json

  [KafkaProducer] - {'bootstrap.servers': 'kafka1:9092,kafka2:9093,kafka3:9094', 'group.id': 'ProductsProducer'}
  {'product_id': 'P01', 'description': 'Carrots', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 1}
  {'product_id': 'P02', 'description': 'Banana', 'target_temperature': 6, 'target_humidity_level': 0.6, 'content_type': 2}
  {'product_id': 'P03', 'description': 'Salad', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 1}
  {'product_id': 'P04', 'description': 'Avocado', 'target_temperature': 6, 'target_humidity_level': 0.4, 'content_type': 1}
  {'product_id': 'P05', 'description': 'Tomato', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 2}
  [KafkaProducer] - Message delivered to products [0]
  [KafkaProducer] - Message delivered to products [0]
  [KafkaProducer] - Message delivered to products [0]
  [KafkaProducer] - Message delivered to products [0]
  [KafkaProducer] - Message delivered to products [0]
  ```

* To validate the data are in the source topic we can use the kafka console consumer. Here are the basic commands:
  
  ```shell
  docker run -ti -v $(pwd):/home --network kafkanet strimzi/kafka:latest-kafka-2.4.0 bash
  $ cd bin
  $ ./kafka-console-consumer.sh --bootstrap-server kafka1:9092 --topic products --from-beginning
  ```

* Define the event streams cluster properties file for the different Kafka tool commands. Set the password attribute of the `jaas.config` to match Event Streams APIKEY. The `eventstream.properties` file looks like:

```properties
bootstrap.servers=broker-3-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-1-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-0-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-5-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-2-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-4-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093
security.protocol=SASL_SSL
ssl.protocol=TLSv1.2
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password=....;
```  

* Restart the `kafka-console-consumer` with the bootstrap URL to access to Event Streams and with the replicated topic name: `source.products`. Use the previously created properties file to get authentication properties so the command looks like:

  ```shell
  source /home/setenv.sh
  ./kafka-console-consumer.sh --bootstrap-server $KAFKA_TARGET_BROKERS --consumer.config /home/eventstream.properties --topic source.products --from-beginning
  ```

* Now we are ready to start Mirror Maker 2.0, close to the local cluster, which is your laptop, using yet another docker image:

```shell
docker run -ti -v $(pwd):/home --network kafkanet strimzi/kafka:latest-kafka-2.4.0 bash
$ /home/local-cluster/launchMM2.sh
```

*This `launchMM2.sh` script is updating a template properties file with the values of the environment variables and calls with this updated file: `/opt/kafka/bin/connect-mirror-maker.sh mm2.properties`*

The trace includes a ton of messages, which displays different Kafka connect consumers and producers, workers and tasks. The logs can be found in the `/tmp/logs` folder within the docker container. The table includes some of the elements of this configuration:

| Name | Description |
| --- | --- |
| Worker clientId=connect-2, groupId=target-mm2 | Herder for target cluster topics but reading source topic|
| Producer clientId=producer-1 | Producer to taget cluster |
| Consumer clientId=consumer-target-mm2-1, groupId=target-mm2] | Subscribed to 25 partition(s): mm2-offsets.target.internal-0 to 24 |
| Consumer clientId=consumer-target-mm2-2, groupId=target-mm2] | Subscribed to 5 partition(s): mm2-status.target.internal-0 to 4 |
| Consumer clientId=consumer-target-mm2-3, groupId=target-mm2] | Subscribed to partition(s): mm2-configs.target.internal-0 |
| Worker clientId=connect-2, groupId=target-mm2 . Starting connectors and tasks using config offset 6.|  This trace shows mirror maker will start to consume message from the offset 6. A previous run has already committed the offset for this client id. This illustrate a Mirror Maker restarts |
| Starting connector MirrorHeartbeatConnector and Starting task MirrorHeartbeatConnector-0  |  |
| Starting connector MirrorCheckpointConnector | |
| Starting connector MirrorSourceConnector | |

As expected, in the consumer console we can see the 5 product messages arriving to the `source.topics` after the replication complete.

```shell
{'bootstrap.servers': 'kafka1:9092,kafka2:9093,kafka3:9094', 'group.id': 'ProductsProducer'}
  {'product_id': 'P01', 'description': 'Carrots', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 1}
  {'product_id': 'P02', 'description': 'Banana', 'target_temperature': 6, 'target_humidity_level': 0.6, 'content_type': 2}
  {'product_id': 'P03', 'description': 'Salad', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 1}
  {'product_id': 'P04', 'description': 'Avocado', 'target_temperature': 6, 'target_humidity_level': 0.4, 'content_type': 1}
  {'product_id': 'P05', 'description': 'Tomato', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 2}
```

## Scenario 2: Run Mirror Maker 2 Cluster close to target cluster

This scenario is similar to the scenario 1 but Mirror Maker 2.0 now, runs within an OpenShift cluster in the same data center as Event Streams cluster, so closer to the target cluster:

![Local to ES](images/mm2-local-to-es.png)

We have created an Event Streams cluster on Washington DC data center. We have Strimzi operators deployed in Washington data center OpenShift Cluster (see [this note](provisioning.md) to provision such environment).

Producers are running locally on the same OpenShift cluster, where vanilla Kafka 2.4 is running, or can run remotely using exposed Kafka brokers Openshift route. The black rectangles in the figure above represent those producers.

The goal is to replicate the `products` topic from the left to the `source.products` to the right.

What needs to be done:

* Get a OpenShift cluster in the same data center as Event Streams service: See this [product introduction](https://cloud.ibm.com/kubernetes/catalog/about?platformType=openshift). It is used to deploy Mirror Maker 2, but for our test we use it as source cluster for replication too.
* Get the API KEY with manager role for event streams cluster and define a kubernetes secret: 

* Run Mirror Maker 2 with the configuration as define in the file: ``. See next section for starting the MME or, read also some mirror maker 2 deployment details in this [provisioning note](mm2-provisioning.md). 
* Start consumer on `source.products` topic
* Run a producer to source topic named `products`

### Run mirror maker 2

* Create a topic to the target cluster: `mm2-offset-syncs.kafka-on-premise-cluster.internal` 

```
oc apply -f local-cluster/kafka-to-es-mm2.yml 
```

A new pod is created or if you have an existing mirror maker 2 depoyed the new configuration is added to your cluster.

### Run Consumer

To validate the replication works, we will connect a consumer to the `source.products` topic on Event Streams. So we define a target cluster property file (`eventstreams.properties`) like:

```properties
security.protocol=SASL_SSL
ssl.protocol=TLSv1.2
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password="am_...";
```

* Start the consumer on `source.products` topic running in Event Streams on the cloud: we use a `setenv.sh` shell to export the needed environment variables

```shell
docker run -ti -v $(pwd):/home strimzi/kafka:latest-kafka-2.4.0 bash
bash-4.2$ source /home/setenv.sh
bash-4.2$ ./bin/kafka-console-consumer.sh --bootstrap-server $KAFKA_TARGET_BROKERS --consumer.config /home/mirror-maker-2/eventstream.properties --topic source.products --from-beginning
```

### Produce records to local cluster

* Start a producer to send product records to the source Kafka cluster. If you have done the scenario 1, the first product definitions may be already in the target cluster, so we can send a second batch of products using a second data file:

```shell
# Under the mirror-maker-2 folder
export KAFKA_BROKERS="eda-demo-24-cluster-kafka-bootstrap-jb-kafka-strimzi.gse-eda-demos-fa9ee67c9ab6a7791435450358e564cc-0001.us-east.containers.appdomain.cloud:443"
export KAFKA_CERT="/home/ca.crt"
docker run -ti -v $(pwd):/home --rm -e KAFKA_CERT=$KAFKA_CERT -e KAFKA_BROKERS=$KAFKA_BROKERS strimzi/kafka:latest-kafka-2.4.0 bash -c "/opt/kafka/bin/kafka-console-producer.sh --broker-list $KAFKA_BROKERS --producer.config /home/kafka-strimzi.properties --topic products"
```

As an alternate solution you can run the producer as a pod inside of the source cluster then send the product one by one using the console:

```shell
oc run kafka-producer -ti --image=strimzi/kafka:latest-kafka-2.4.0  --rm=true --restart=Never -- bin/kafka-console-producer.sh --broker-list eda-demo-24-cluster-kafka-bootstrap:9092 --topic products
If you don t see a command prompt, try pressing enter.

>{'product_id': 'P01', 'description': 'Carrots', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 1}
>{'product_id': 'P02', 'description': 'Banana', 'target_temperature': 6, 'target_humidity_level': 0.6, 'content_type': 2}
>{'product_id': 'P03', 'description': 'Salad', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 1}
>{'product_id': 'P04', 'description': 'Avocado', 'target_temperature': 6, 'target_humidity_level': 0.4, 'content_type': 1}
>{'product_id': 'P05', 'description': 'Tomato', 'target_temperature': 4, 'target_humidity_level': 0.4, 'content_type': 2}
```

!!! Note
      There is other solution to send records, like using a Kafka HTTP brigde and use `curl post` commands.

* To validate the source `products` topic has records, start a consumer as pod on Openshift within the source Kafka cluster using the Strimzi/kafka image.

```shell
oc run kafka-consumer -ti --image=strimzi/kafka:latest-kafka-2.4.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic products --from-beginning
```
