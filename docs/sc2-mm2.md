# Running a custom Mirror Maker 2.0

In this approach we are using properties file to define the Mirror Maker 2.0 configuration, package JMX exporter with it inside a docker image and deploy the image to Openshift.
The configuration approach supports the replication from local on-premise cluster running on kubernetes cluster to Event Streams on cloud.

![Local to ES](images/mm2-local-to-es.png)

## Define the MM configuration

The configuration define the source and target cluster and the security settings for both clusters. As the goal is to run within the same OpenShift cluster as Kafka, the broker list for the source matches the URL within the broker service:

```shell
# get the service URL
oc describe svc my-cluster-kafka-bootstrap
# URL my-cluster-kafka-bootstrap:9092
```
The target cluster uses the bootstrap servers from the Event Streams Credentials, and the API KEY is defined with the manager role, so mirror maker can create topic dynamically.

Properties template file can be seen [here](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/local-cluster/localkafka-to-es-mm2.properties)

```
clusters=source, target
source.bootstrap.servers=my-cluster-kafka-bootstrap:9092
source.ssl.endpoint.identification.algorithm=
target.bootstrap.servers=broker-3-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-1-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-0-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-5-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-2-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-4-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093
target.security.protocol=SASL_SSL
target.ssl.protocol=TLSv1.2
target.ssl.endpoint.identification.algorithm=https
target.sasl.mechanism=PLAIN
target.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password="<Manager API KEY from Event Streams>";
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

Upload the properties as a secret

```shell
oc create secret generic mm2-std-properties --from-file=kafka-to-es.properties
```

## Defining a custom docker image

Starting from the Kafka 2.4 image, we need to add Prometheus JMX exporter and use the properties file as argument. The docker file is [here](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/Dockerfile).

```Dockerfile
FROM strimzi/kafka:latest-kafka-2.4.0
# ...
ENV LOG_DIR=/tmp/logs
ENV EXTRA_ARGS="-javaagent:/usr/local/share/jars/jmx_prometheus_javaagent-0.12.0.jar=9400:/etc/jmx_exporter/jmx_exporter.yaml "

EXPOSE 9400

CMD /opt/kafka/bin/connect-mirror-maker.sh  /home/kafka-to-es.properties
```

The file could be copied inside the docker image or better mounted from secret when deployed to kubernetes.

Build and push the image to a docker registry.

```shell
docker build -t ibmcase/mm2ocp:v0.0.2 .
docker push ibmcase/mm2ocp:v0.0.2
```

## Define the monitoring rules

As explained in the [monitoring note](monitoring.md), we need to define the Prometheus rules within a [yaml file](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/mm2-jmx-exporter.yaml):

```yaml
lowercaseOutputName: true
lowercaseOutputLabelNames: true
rules:
  - pattern : "kafka.connect<type=connect-worker-metrics>([^:]+):"
    name: "kafka_connect_connect_worker_metrics_$1"
  - pattern : "kafka.connect<type=connect-metrics, client-id=([^:]+)><>([^:]+)"
    name: "kafka_connect_connect_metrics_$1_$2"   
  # Rules below match the Kafka Connect/MirrorMaker MBeans in the jconsole order
  # Worker task states
  - pattern: kafka.connect<type=connect-worker-metrics, connector=(\w+)><>(connector-destroyed-task-count|connector-failed-task-count|connector-paused-task-count|connector-running-task-count|connector-total-task-count|connector-unassigned-task-count)
    name: connect_worker_metrics_$1_$2
  # Task metrics
  - pattern: kafka.connect<type=connector-task-metrics, connector=(\w+), task=(\d+)><>(batch-size-avg|batch-size-max|offset-commit-avg-time-ms|offset-commit-failure-percentage|offset-commit-max-time-ms|offset-commit-success-percentage|running-ratio)
    name: connect_connector_task_metrics_$1_$3
    labels:
       task: "$2"
  # Source task metrics
  - pattern: kafka.connect<type=source-task-metrics, connector=(\w+), task=(\d+)><>(source-record-active-count|source-record-poll-total|source-record-write-total)
    name: connect_source_task_metrics_$1_$3
    labels:
       task: "$2"
  # Task errors
  - pattern: kafka.connect<type=task-error-metrics, connector=(\w+), task=(\d+)><>(total-record-errors|total-record-failures|total-records-skipped|total-retries)
    name: connect_task_error_metrics_$1_$3
    labels:
      task: "$2"
  # CheckpointConnector metrics 
  - pattern: kafka.connect.mirror<type=MirrorCheckpointConnector, source=(.+), target=(.+), group=(.+), topic=(.+), partition=(\d+)><>(checkpoint-latency-ms)
    name: connect_mirror_mirrorcheckpointconnector_$6
    labels:
       source: "$1"
       target: "$2"
       group: "$3"
       topic: "$4"
       partition: "$5"
  # SourceConnector metrics
  - pattern: kafka.connect.mirror<type=MirrorSourceConnector, target=(.+), topic=(.+), partition=(\d+)><>(byte-rate|byte-count|record-age-ms|record-rate|record-count|replication-latency-ms)
    name: connect_mirror_mirrorsourceconnector_$4
    labels:
       target: "$1"
       topic: "$2"
       partition: "$3"
```

Then upload this properties file in a secret (the following command update an existing config as predefined by the Prometheus deployment)

```shell
oc create secret generic mm2-jmx-exporter --from-file=./mm2-jmx-exporter.yaml
```

## Deploy the application

As we are using secret to mount file we want to use a deployment.yml to define the Mirror Maker deployment

```shell
oc apply -f mm2-deployment.yaml
```

To undeploy everything

```shell
oc delete all -l app=mm2ocp
```

