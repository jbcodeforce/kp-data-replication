# To Event Streams to Kafka cluster on-premise

## Scenario 3: From Event Streams to local kafka cluster

For this scenario the source is Event Streams on IBM Cloud and the target is a local Kafka cluster.

![Scenario 3](images/mm2-scen3.png)

As a prerequisite you need to run your local cluster, for example using docker compose as introduced in [this note](dc-local.md).

This time the producer adds headers to the Records sent so we can validate headers replication. The file [es-cluster/es-mirror-maker.properties](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/es-cluster/es-mirror-maker.properties) declares the mirroring settings as below:

```properties
clusters=source, target
source.bootstrap.servers=broker-3-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-1-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-0-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-5-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-2-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-4-qnprtqnp7hnkssdz.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093
source.security.protocol=SASL_SSL
source.ssl.protocol=TLSv1.2
source.sasl.mechanism=PLAIN
source.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password="985...";
target.bootstrap.servers=kafka1:9092,kafka2:9093,kafka3:9094
# enable and configure individual replication flows
source->target.enabled=true
source->target.topics=orders
```

* Start [mirror maker2.0](https://cwiki.apache.org/confluence/display/KAFKA/KIP-382%3A+MirrorMaker+2.0):

    By using a new container, start another kakfa 2.4+ docker container, connected to the  brokers via the `kafkanet` network, and mounting the configuration in the `/home`:

    ```shell
    docker run -ti --network kafkanet -v $(pwd):/home strimzi/kafka:latest-kafka-2.4.0 bash
    ```

    Inside this container starts mirror maker 2.0 using the script: `/opt/kakfa/bin/connect-mirror-maker.sh`

    ```shell
    /opt/kakfa/bin/connect-mirror-maker.sh /home/strimzi-mm2.properties
    ```

    The `strimzi-mm2.properties` properties file given as argument defines the source and target clusters and the topics to replicate.

* The consumer may be started in second or third step. To start it, you can use a new container or use one of the running kafka broker container. Using the `Docker perspective` in Visual Code, we can get into a bash shell within one of the Kafka broker container. The local folder is mounted to `/home`. Then the script, `consumeFromLocal.sh source.orders` will get messages from the replicated topic: `source.orders`

## Scenario 4: From Event Streams On Cloud to Strimzi Cluster on Openshift

We are reusing the Event Streams on Cloud cluster on Washington DC data center as source target and the vanilla Kafka 2.4 cluster as target, also running within Washington data center in a OpenShift Cluster. As both clusters are in the same data center, we deploy Mirror Maker 2.0 close to target kafka cluster.

![](images/mm2-test1.png)

1. Deploy mirror maker 2.0 with good configuration: As we use the properties file approach the Dockerfile helps us to build a custom MM2 with Prometheus JMX exporter and mm2.properties for configuration. The file specifies source and target cluster:

    ```properties
    clusters=source, target
    target.bootstrap.servers=eda-demo-24-cluster-kafka-bootstrap:9092
    target.ssl.endpoint.identification.algorithm=
    source.bootstrap.servers=<REPLACE-WITH-ES-BROKERSLIST>
    source.security.protocol=SASL_SSL
    source.ssl.protocol=TLSv1.2
    source.ssl.endpoint.identification.algorithm=https
    source.sasl.mechanism=PLAIN
    source.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password="<REPLACE-WITH-ES-APIKEY>";
    # enable and configure individual replication flows
    source->target.enabled=true
    sync.topic.acls.enabled=false
    replication.factor=3
    internal.topic.replication.factor=3
    refresh.topics.interval.seconds=10
    refresh.groups.interval.seconds=10
    source->target.topics=products
    tasks.max=10
    ```

    Start Mirror Maker 2.0: we use the properties setting one with custom docker image.

    ```shell
    oc apply -f mirror-maker/mm2-deployment.yaml
    ```

1. Produce some records to `products` topic on Event Streams. For that create a properties file (`eventstream.properties`) with the event streams API KEY and SASL_SSL properties:

    ```properties
    security.protocol=SASL_SSL
    ssl.protocol=TLSv1.2
    sasl.mechanism=PLAIN
    sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password="...."
    ```

    Then starts a local container with Kafka 2.4 and console producer:
    
    ```shell
    export KAFKA_BROKERS="event streams broker list"
    docker run -ti -v $(pwd):/home --rm -e KAFKA_BROKERS=$KAFKA_BROKERS strimzi/kafka:latest-kafka-2.4.0 bash -c "/opt/kafka/bin/kafka-console-producer.sh --broker-list $KAFKA_BROKERS --producer.config /home/eventstreams.properties --topic products"
    > 
    ```

    For the data, you can use any text, or the products we have in the data folder.

1. To validate the target `source.products` topic has records, start a consumer as pod on Openshift within the source Kafka cluster using the Strimzi/kafka image.

  ```shell
  oc run kafka-consumer -ti --image=strimzi/kafka:latest-kafka-2.4.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic source.products --from-beginning

  If you don't see a command prompt, try pressing enter.

  { "product_id": "P01", "description": "Carrots", "target_temperature": 4,"target_humidity_level": 0.4,"content_type": 1}
  { "product_id": "P02", "description": "Banana", "target_temperature": 6,"target_humidity_level": 0.6,"content_type": 2}
  { "product_id": "P03", "description": "Salad", "target_temperature": 4,"target_humidity_level": 0.4,"content_type": 1}
  ```

## Considerations

When the source or target cluster is deployed on Openshift, the exposed route to access the brokers is using TLS connection. So we need the certificate and create a truststore to be used by the consumer or producer Java programs. All kafka tools are done in java or scala so running in a JVM, which needs truststore for keep trusted TLS certificates.

To get the certificate do the following steps:

1. Get the host ip address from the Route resource

    ```shell
    oc get routes my-cluster-kafka-bootstrap -o=jsonpath='{.status.ingress[0].host}{"\n"}'
    ```

1. Get the TLS CA root certificate from the broker

    ```shell
    oc get secrets
    oc extract secret/my-cluster-cluster-ca-cert --keys=ca.crt --to=- > ca.crt
    ```

1. Transform the certificate for java truststore

    ```shell
    keytool -import -trustcacerts -alias root -file ca.crt -keystore truststore.jks -storepass password -noprompt
    ```
