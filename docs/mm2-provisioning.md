# Mirror Maker 2 Deployment

In this article we are presenting different type of Mirror Maker 2 deployments. Updated 4/4 on Strimzi version 0.17.

* Using Strimzi operator to deploy on Kubernetes
* To run MM2 on a VM or as docker image which can be adapted with your own configuration, like for example by adding prometheus JMX Exporter as java agent.

We are using the configuration to deploy from event streams on Cloud to a local Kafka cluster we deployed using Strimzi.

![ES to local](images/mm2-test1.png)

## Common configuration

When we need to create Kubernetes `Secrets` to manage APIKEY to access Event Streams, and TLS certificate to access local Kafka brokers, we need to do the following steps:

* Create a project in OpenShift to deploy Mirror Maker cluster, for example: `oc new-project <projectname>`.
* Create a secret for the API KEY of the Event Streams cluster:
`oc create secret generic es-api-secret --from-literal=password=<replace-with-event-streams-apikey>`
* As your vanilla Kafka source cluster may use TLS to communicate between clients and brokers, you need to use the k8s secret defined when deploying Kafka which includes the CAroot and generic client certificates. These secrets are : `eda-demo-24-cluster-clients-ca-cert` and  `eda-demo-24-cluster-cluster-ca-cert`.

1. Get the host ip address from the Route resource

    ```shell
    oc get routes my-cluster-kafka-bootstrap -o=jsonpath='{.status.ingress[0].host}{"\n"}'
    ```

1. Get the TLS CA root certificate from the broker

    ```shell
    oc get secrets
    oc extract secret/eda-demo-24-cluster-cluster-ca-cert --keys=ca.crt --to=- > ca.crt
    oc extract secret/eda-demo-24-cluster-clients-ca-cert --keys=ca.crt --to=- >> ca.crt
    ```

1. Transform the certificates for java truststore

    ```shell
    keytool -import -trustcacerts -alias root -file ca.crt -keystore truststore.jks -storepass password -noprompt
    ```

1. Create a secret from truststore file so it can be mounted as needed into consumer or producer running in the same OpenShift cluster. 

  ```shell
  oc create secret generic kafka-truststore --from-file=./truststore.jks
  ```

## Deploying using Strimzi Mirror Maker operator

We assume you have an existing namespace or project to deploy Mirror Maker. You also need to get the latest (0.17-rc4 at least) Strimzi configuration from the [download page](https://github.com/strimzi/strimzi-kafka-operator/releases/tag/0.17.0-rc4).

If you have already installed Strimzi Operators, Cluster Roles, and CRDs, you do not need to do it again as those resources are defined at the kubernetes cluster level. See the [provisioning note.](provisioning.md)

* Define source and target cluster properties in mirror maker 2.0 `es-to-kafka-mm2.yml` descriptor file. Here is the file for the replication between Event Streams and local cluster [es-to-kafka-mm2.yml](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/es-cluster/es-to-kafka-mm2.yml). We strongly recommend to study the schema definition of this [custom resource from this page](https://github.com/strimzi/strimzi-kafka-operator/blob/2d35bfcd99295bef8ee98de9d8b3c86cb33e5842/install/cluster-operator/048-Crd-kafkamirrormaker2.yaml#L648-L663). 

!!! note
    `connectCluster` attribute defines the cluster alias used for Kafka Connect, it must match a cluster in the list at `spec.clusters`.
    The config part can match the Kafka configuration for consumer or producer, except properties starting by ssl, sasl, security, listeners, rest, bootstarp.servers which are declared at the cluster definition level. 

```yaml
  alias: event-streams-wdc-as-target
    bootstrapServers: broker-3...
    tls: {}
    authentication:
      passwordSecret:
          secretName: es-api-secret  
          password: password
      username: token
      type: plain
```

The example above use a secret to get the Event Streams API KEY, which as create with a command like: `oc create secret generic es-api-secret --from-literal=password=<replace with ES key>`

* Deploy Mirror maker 2.0 within your project.

```shell
oc apply -f es-to-kafka-mm2.yml
```

This commmand creates a kubernetes deployment as illustrated below, with one pod as the replicas is set to 1. If we need to add parallel processing because of the topics to replicate have multiple partitions, or there are a lot of topics to replicate, then adding pods will help to scale horizontally. The pods are in the same consumer group, so Kafka Brokers will do the partition rebalancing among those new added consumers.

![Mirror maker deployment](images/mm2-deployment.png)

Now with this deployment we can test consumer and producer as described [in the scenario 4](es-to-local/#scenario-4-from-event-streams-on-cloud-to-strimzi-cluster-on-openshift).

## MM2 topology

In this section we want to address horizontal scalability and how to organize the MirrorMaker 2 topology for multi tenancy. The simplest approach is to use one MirrorMaker instance per familly of topics: the classification of familly of topic can be anything, from line of business, to team, to application. Suppose an application is using 1000 topic - partitions, for data replication it may make sense to have one MM2 instance for this application responsible to manage the topics replication. The configuration will define the groupId to match the application name for example.

The following diagram illustrates this kind of topology by using regular expression on the topic white list selection, there are three MirrorMaker 2 instances mirroring the different topics with name starting with `topic-name-A*, topic-name-B*, topic-name-C*,` respectively.

![](images/mm2-topology.png)

Each connect instance is a JVM workers that replicate the topic/parititions and has different group.id.

For Bi-directional replication for the same topic name, MirrorMaker 2 will use the cluster name as prefix. With MM2 we do not need to have 2 clusters but only one and bidirectional definitions. The following example is showing the configuration for a MM2 bidirectional settings, with `accounts` topic to be replicated on both cluster:

```
apiVersion: kafka.strimzi.io/v1alpha1
kind: KafkaMirrorMaker2
...
 mirrors:
  - sourceCluster: "event-streams-wdc"
    targetCluster: "kafka-on-premise"
    ...
    topicsPattern: "accounts,orders"
  - sourceCluster: "kafka-on-premise"
    targetCluster: "event-streams-wdc"
    ...
    topicsPattern: "accounts,customers"
```

## Capacity planning

To address capacity planning, we need to review some characteristic of the Kafka Connect framework: For each topic/partition there will be a task running. We can see in the trace that tasks are mapped to threads inside the JVM. So the parallelism will be bound by the number of CPUs the JVM runs on. The parameters `max.tasks` specifies the max parallel processing we can have per JVM. So for each Topic we need to assess the number of partitions to be replicated. Each task is using the consumer API and is part of the same consumer group, the partition within a group are balanced by an internal controler. With Kafka connect any changes to the topic topology triggers a partition rebalancing. In MM2 each consumer / task is assigned a partition by the controller. So the rebalancing is done internally. Still adding a broker node into the cluster will generate rebalancing.

The task processing is stateless, consume - produce wait for acknowledge,  commit offet. In this case the CPU and network are key. For platform tuning activity we need to monitor operating system performance metrics. If the CPU becomes the bottleneck, we can allocate more CPU or start to scale horizontally by adding mirror maker 2 instance. If the network at the server level is the bottleneck, then adding more servers will help. Kafka will automatically balance the load among all the tasks running on all the machines. The size of the message impacts also the throughtput as with small message the throughput is CPU bounded. With 100 bytes messages or more we can observe network saturation. 

The parameters to consider for sizing are the following:

| Parameter | Description |Impact|
| --- | --- | --- |
| Number of topic/ partition | Each task processes one partition | For pure parallel processing max.tasks is the number of CPU |
| Record size | Size of the message in each partition in average | Memory usage and Throughput: the # of records/s descrease when size increase, while MB/s throughput increases in logarithmic|
| Expected input throughput | The producer writing to the source topic throughput | Be sure the consumers inside MM2 absorb the demand |
| Network latency | | This is where positioning MM2 close to the target cluster may help improve latency|

## Version migration

Once the MirrorMaker cluster is up and running, it may be needed to update the underlying code when a new product version is released. Based on Kafka Connect distributed mode multiple workers JVM coordinate the topic / partition repartition among themselves via Kafka topic.  If a worker process dies, the cluster is rebalanced to distribute the work fairly over the remaining workers.
If a new worker starts work, a rebalance ensures it takes over some work from the existing workers.

Using the REST API it is possible to stop and restart a connector. As of now the recommendation is to start a new MirrorMaker instance with the new version and the same groupId as the existing workers you want to migrate. Then stop the existing version. As each MirrorMaker workers are part of the same group, the internal worker controller will coordinate with the other workers the  'consumer' task to partition assignment.

When using Strimzi, if the update applies to the MM2 Custom Resource Definition, just reapplying the CRD should be enough.

Be sure to verify the product documentation as new version may enforce to have new topics. It was the case when Kafka connect added the config topic in a recent version.

## Deploying a custom MirrorMaker docker image

We want to use custom docker image when we want to add Prometheus JMX exporter as Java Agent so we can monitor MM2 with Prometheus. The proposed docker file is [in this folder](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/Dockerfile) and may look like:

```Dockerfile
FROM strimzi/kafka:latest-kafka-2.4.0
# ...
ENV LOG_DIR=/tmp/logs
ENV EXTRA_ARGS="-javaagent:/usr/local/share/jars/jmx_prometheus_javaagent-0.12.0.jar=9400:/etc/jmx_exporter/jmx_exporter.yaml "

# ....
EXPOSE 9400

CMD /opt/kafka/bin/connect-mirror-maker.sh  /home/mm2.properties
```

As the mirror maker 2 is using properties file, we want to define source and target cluster and the security settings for both clusters. As the goal is to run within the same OpenShift cluster as Kafka, the broker list for the source matches the URL within the broker service:

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

The file could be copied inside the docker image or better mounted from a secret when deployed to kubernetes.

Build and push the image to a docker registry.

```shell
docker build -t ibmcase/mm2ocp:v0.0.2  .
docker push ibmcase/mm2ocp:v0.0.2
```

Then using a deployment configuration like [this one](https://github.com/jbcodeforce/kp-data-replication/blob/master/mirror-maker-2/mm2-deployment.yaml), we can deploy our custom mirror maker 2 with:

```shell
oc apply -f mm2-deployment.yaml
# to assess the cluster 
oc get kafkamirrormaker2
NAME          DESIRED REPLICAS
mm2-cluster   1

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


## Deploying Mirror Maker 2 on its own project

In this section we address another approach to, deploy a Kafka Connect cluster with Mirror Maker 2.0 connectors but without any local Kafka Cluster. The approach may be used with Event Streams on Cloud as backend Kafka cluster and Mirror Maker 2 for replication.

Using the Strimzi operator we need to define a Yaml file for the connector and white and black lists for the topics to replicate. Here is an [example of such descriptor]().

If we need to run a custom Mirror Maker 2, we have documented in [the section above](#) on how to use Dockerfile and properties file and deployment descriptor to do the deployment on kubernetes or OpenShift cluster.

## Provisioning automation

For IT operation automation we can use [Ansible](https://www.ansible.com/resources/videos/quick-start-video) to define a playbook to provision the Mirror Maker 2 environment. The [Strimzi Ansible playbook](https://github.com/rmarting/strimzi-ansible-playbook) repository containts playbook examples for creating cluster roles and service accounts and deploy operators.

The automation approach will include:

* Deploy all cluster objects needed into a OpenShift cluster: Cluster Roles, Strimzi CRDs: Kafka, KafkaTopic, KafkaUser, Kafka connect, mirror maker(s).
* Deploy all namespaced objects needed into an OpenShift namespace: Service Accounts, Cluster Role Bindings and Role Bindings, Cluster Operator deployment.
* Deploy the kafka cluster if needed.
* Deploy the different mirror maker 2 instances.

## Typical errors in Mirror Maker 2 traces

* Plugin class loader for connector: 'org.apache.kafka.connect.mirror.MirrorCheckpointConnector' was not found. 
    * This error message is a light issue in kafka 2.4 and does not impact the replication. In Kafka 2.5 this message is for DEBUG logs.
* Error while fetching metadata with correlation id 2314 : {source.heartbeats=UNKNOWN_TOPIC_OR_PARTITION}:
    * Those messages may come from multiple reasons. One is that the named topic is not created. In Event Streams is the target cluster the topics may need to be created via CLI or User Interface. It can also being related to the fact the consumer polls on a topic that has just been created and the leader for this topic-partition is not yet available, you are in the middle of a leadership election.
    * The advertised listener may not be set or found.
* Exception on not being able to create Log directory: do the following: `export LOG_DIR=/tmp/logs`
* ERROR WorkerSourceTask{id=MirrorSourceConnector-0} Failed to flush, timed out while waiting for producer to flush outstanding 1 messages
* ERROR WorkerSourceTask{id=MirrorSourceConnector-0} Failed to commit offsets (org.apache.kafka.connect.runtime.SourceTaskOffsetCommitter:114)

**Some useful commands**

* Connect to local cluster: `oc exec -ti eda-demo-24-cluster-kafka-0 bash`
* list the topics: `./kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --list`
* Get the description of the topics from one cluster:

```
 for t in $(./kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --list)
 do 
 ./kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --describe --topic $t
 done
```

## Create MM2 topics manually

Here are some examples of command to create topic to the target cluster

If you want to delete the topic on your local cluster
```
 for t in $(/opt/kafka/bin/kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --list)
 do 
 ./kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --delete --topic $t
 done
```

To create the topics manually on the target cluster:exit

```
/opt/kafka/bin/kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --create --partitions 5 --topic mm2-offset-syncs.kafka-on-premise-cluster-source.internal

/opt/kafka/bin/kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --create  --partitions 5 --replication-factor 3 --topic mirrormaker2-cluster-status

/opt/kafka/bin/kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --create  --partitions 25 --replication-factor 3 --topic mirrormaker2-cluster-offsets

/opt/kafka/bin/kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --create  --partitions 1 --replication-factor 3  --topic mirrormaker2-cluster-configs

/opt/kafka/bin/kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --create  --partitions 1 --replication-factor 3  --topic heartbeats

/opt/kafka/bin/kafka-topics.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap:9092 --create  --partitions 1 --replication-factor 1  --topic  event-streams-wdc.checkpoints.internal

```