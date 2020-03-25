# Mirror Maker 2 Deployment

In this article we are presenting different type of Mirror Maker 2 deployments. Updated 3/24 on Strimzi 017 rc4.

* Using Strimzi operator to deploy on Kubernetes
* To run in VM or docker image which can be adapted with your own configuration, like for example by adding prometheus JMX Exporter as java agent.

We are adding as part of the custom 
we are using properties files to define the Mirror Maker 2.0 configuration, package JMX exporter with it inside a docker image and deploy the image to Openshift.
The configuration approach supports the replication from local on-premise cluster running on kubernetes cluster to Event Streams on the Cloud.

![Local to ES](images/mm2-local-to-es.png)

## Common configuration

When we need to create Kubernetes secrets to manage APIKEY to access Event Streams, and TLS certificate to access local Kafka brokers, we need to do the following steps:

* Create a project in OpenShift to deploy Mirror Maker cluster, for example: `oc new-project <projectname>`.
* Create a secret for the API KEY of the Event Streams cluster:
`oc create secret generic es-api-secret --from-literal=password=<replace-with-event-streams-apikey>`
* As your vanilla Kafka source cluster may use TLS to communicate between clients and brokers, you need to use the k8s secret defined when deploying Kafka which includes the CAroot certificate. This secret is : `my-cluster-clients-ca-cert`.

```shell
# build a local CA crt file from the secret:
oc extract secret/my-cluster-clients-ca-cert --keys=ca.crt --to=- > ca.crt
# Verify the certificate:
openssl x509 -in ca.crt -text
# transform it for java truststore.jks:
keytool -import -trustcacerts -alias root -file ca.crt -keystore truststore.jks -storepass password -noprompt
# create a secret from file the truststore so it can be mounted as needed
oc create secret generic kafka-truststore --from-file=./truststore.jks
# Verify the created secret
oc describe secret kafka-truststore
```

!!! Attention
    At this step, we have two options to deploy mirror maker, one using the Mirror Maker Operator and configure it via a yaml file, or use properties file and a special docker image that is deployed to Openshift. As of 3/20/2020 we have found an issue on Strimzi 0.17-rc2 Mirror Maker 2.0 operator, so we are proposing to use the properties approach as [documented this section](#deploying-a-customer-mirror-maker-docker-image).

## Deploying using Strimzi Mirror Maker operator

We assume you have an existing namespace or project to deploy Mirror Maker. You also need to get the latest (0.17-rc4 at least) Strimzi configuration from the [download page](https://github.com/strimzi/strimzi-kafka-operator/releases/tag/0.17.0-rc4).

If you have already installed Strimzi Cluster Roles, and CRDs and operator you do not need to do it again as those resources are at the kubernetes cluster level.

* As service accounts and role bindings are local to a project do the following

  ```shell
  export INSTALLDIR=$STRIMZI_HOME/install
  export NSPACE="yourprojectname"
  export TGTDIR=$(pwd)
  cp $INSTALLDIR/cluster-operator/*RoleBinding*.yaml $TGTDIR
  oc apply -f $INSTALLDIR/cluster-operator/*ServiceAccount*.yaml
  sed -i '' "s/namespace: .*/namespace: "$NSPACE"/"  $TGTDIR/*RoleBinding*.yaml
  oc apply -f $TGTDIR
  ```

  At this stage you should have CRDs, Operators, ClusterRoles, and service accounts:

* Define source and target cluster properties in mirror maker 2.0 `mm2.yml` descriptor file. We strongly recommend to study the schema definition of this [custom resource from this page](https://github.com/strimzi/strimzi-kafka-operator/blob/2d35bfcd99295bef8ee98de9d8b3c86cb33e5842/install/cluster-operator/048-Crd-kafkamirrormaker2.yaml#L648-L663). The [yaml file we used is here](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/local-cluster/kafka-to-es-mm2.yml).

!!! note
    `connectCluster` attribute defines the cluster alias used for Kafka Connect, it must match a cluster in the list at `spec.clusters`.
    The config part can match the Kafka configuration for consumer or producer, except properties starting by ssl, sasl, security, listeners, rest, bootstarp.servers which are declared at the cluster definition level. 

```yaml
  alias: "event-streams-wdc-as-target"
    bootstrapServers: broker-3...
    tls: {}
    authentication:
      passwordSecret:
          secretName: es-api-secret  
          password: password
      username: token
      type: plain
```

* Deploy Mirror maker 2.0 within your project.

```shell
oc apply -f mm2.yaml
```

This commmand creates a kubernetes deployment as illustrated below, with one pod as the replicas is set to 1. If we need to add parallel processing because of the topics to replicate have multiple partitions, or there are a lot of topics to replicate, then adding pods will help to scale horizontally. The pods are in the same consumer group, so Kafka Brokers will do the partition rebalancing among those new added consumers.

![Mirror maker deployment](images/mm2-deployment.png)

## Deploying a custom Mirror Maker docker image

We want to use this approach to use properties file for defining the replication parameters and to add Prometheus JMX exporter as Java Agent so we can monitor MM2 with Prometheus. The proposed docker file is [in this folder](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/Dockerfile) and may look like:

```Dockerfile
FROM strimzi/kafka:latest-kafka-2.4.0
# ...
ENV LOG_DIR=/tmp/logs
ENV EXTRA_ARGS="-javaagent:/usr/local/share/jars/jmx_prometheus_javaagent-0.12.0.jar=9400:/etc/jmx_exporter/jmx_exporter.yaml "

# ....
EXPOSE 9400

CMD /opt/kafka/bin/connect-mirror-maker.sh  /home/mm2.properties
```

The file could be copied inside the docker image or better mounted from a secret when deployed to kubernetes.

Build and push the image to a docker registry.

```shell
docker build -t ibmcase/mm2ocp:v0.0.2  .
docker push ibmcase/mm2ocp:v0.0.2
```

### Define the monitoring rules

As explained in the [monitoring note](monitoring.md), we need to define the Prometheus rules within a [yaml file](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/mm2-jmx-exporter.yaml) so that Mirror Maker 2 can report metrics:

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

Then upload this `yaml` file in a secret (the following command, represents a trick to update an existing configmap)

```shell
oc create secret generic mm2-jmx-exporter --from-file=./mm2-jmx-exporter.yaml
```

## Deploying on VM

On virtual machine, it is possible to deploy the Apache Kafka 2.4+ binary file and then use the command `/opt/kafka/bin/connect-mirror-maker.sh` with the good properties file as argument.

Within a VM we can run multiple mirror maker instances. When needed we can add more VMs to scale horizontally. Each mirror makers workers are part of the same consumer groups, so it is possible to scale at the limit of the topic partition number.

## Capacity planning


## Mirror Maker Scaling considerations


## Define the MM configuration

The configuration define the source and target cluster and the security settings for both clusters. As the goal is to run within the same OpenShift cluster as Kafka, the broker list for the source matches the URL within the broker service:

```shell
# get the service URL
oc describe svc my-cluster-kafka-bootstrap
# URL my-cluster-kafka-bootstrap:9092
```
The target cluster uses the bootstrap servers from the Event Streams Credentials, and the API KEY is defined with the manager role, so mirror maker can create topic dynamically.

Properties template file can be seen [here: kafka-to-es-mm2](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/local-cluster/kafka-to-es-mm2.properties)

```properties
clusters=source, target
source.bootstrap.servers=eda-demo-24-cluster-kafka-bootstrap:9092
source.ssl.endpoint.identification.algorithm=
target.bootstrap.servers=broker-3-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-1-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-0-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-5-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-2-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093,broker-4-h6s2xk6b2t77g4p1.kafka.svc01.us-east.eventstreams.cloud.ibm.com:9093
target.security.protocol=SASL_SSL
target.ssl.protocol=TLSv1.2
target.ssl.endpoint.identification.algorithm=https
target.sasl.mechanism=PLAIN
target.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required 
username="token" password="<Manager API KEY from Event Streams>";
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
oc create secret generic mm2-std-properties --from-file=es-cluster/mm2.properties
```


## From Event Streams on Cloud to Local Cloud

The approach is similar to the above steps except we use another properties file:

* The properties to use is `es-to-kafka-mm2.properties`

```properties
clusters=source, target
target.bootstrap.servers=eda-demo-24-cluster-kafka-bootstrap:9092
target.ssl.endpoint.identification.algorithm=
source.bootstrap.servers=broker-3-h6s2xk6b2t77g4p1.kafka.svc01.us-east.event....
```

* The image to build is using this properties file:

```shell
docker build -t ibmcase/mm2ocp:v0.0.3 --build-arg=es-cluster/es-to-kafka-mm2.properties .
```

As we are using the secret define in section above to mount file we want to use a deployment.yml to define the Mirror Maker deployment

```shell
# Under mirror-maker-2 folder
oc apply -f mm2-deployment.yaml
```

In case of error like "" you may need to create the Kafka Topic

```
/opt/kafka/bi/kafka-topic.sh --create --zookeeper eda-demo-24-cluster-zookeeper:2181 --replication-factor 3 --partitions 25 --topic mm2-offsets.target.internal
```

For the other topic:
| Topic | Partition |
| --- | --- |
| mm2-configs.source.internal | 1 |
} 
To undeploy everything

```shell
oc delete all -l app=mm2ocp
```



## Deploying Mirror Maker 2

In this section we address another approach to, deploy a Kafka Connect cluster with Mirror Maker 2.0 connectors but without any local Kafka Cluster. The approach may be used with Event Streams on Cloud as backend Kafka cluster and Mirror Maker 2 for replication.

Using the Strimzi operator we need to define a Yaml file for the connector and white and black lists for the topics to replicate. Here is an [example of such descriptor]().

If we need to run a custom Mirror Maker 2, we have documented in [this note](mm2-provisioning.md) how to use Dockerfile and properties file and deployment descriptor to do the deployment on kubernetes or OpenShift cluster.

## Typical errors in Mirror Maker 2 traces

* Plugin class loader for connector: 'org.apache.kafka.connect.mirror.MirrorCheckpointConnector' was not found. 
    * This error message is a light issue in kafka 2.4 and does not impact the replication. In Kafka 2.5 this message is for DEBUG logs.
* Error while fetching metadata with correlation id 2314 : {source.heartbeats=UNKNOWN_TOPIC_OR_PARTITION}:
    * Those messages may come from multiple reasons. One is the name topic is not created. In Event Streams topics needs to be created via CLI or User Interface. It can also being related to the fact the consumer polls on a topic that has just been created and the leader for this topic-partition is not yet available, you are in the middle of a leadership election.
    * The advertised listener may not be set or found.
* Exception on not being able to create Log directory: do the following: `export LOG_DIR=/tmp/logs`
* ERROR WorkerSourceTask{id=MirrorSourceConnector-0} Failed to flush, timed out while waiting for producer to flush outstanding 1 messages
* ERROR WorkerSourceTask{id=MirrorSourceConnector-0} Failed to commit offsets (org.apache.kafka.connect.runtime.SourceTaskOffsetCommitter:114)

