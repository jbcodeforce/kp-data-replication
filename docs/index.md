# Introduction

This repository includes a set of documents for best practices around data replication between two Kafka clusters.

## Mirror Maker 2.0

Mirror Maker 2.0 is the new replication feature of Kafka 2.4. It was defined as part of the [KIP 382](https://cwiki.apache.org/confluence/display/KAFKA/KIP-382%3A+MirrorMaker+2.0).

### General concepts

As [Mirror maker 2.0](https://strimzi.io/docs/master/#con-configuring-mirror-maker-deployment-configuration-kafka-mirror-maker) is using kafka Connect framework, we recommend to review our summary [in this note](https://ibm-cloud-architecture.github.io/refarch-eda/kafka/connect/).

The figure below illustrates the mirror maker internal components running within Kafka Connect.

![Kafka Connect](images/mm-k-connect.png)

In distributed mode, Mirror Maker creates the following topics to the target cluster:

* mm2-configs.source.internal: This topic will store the connector and task configurations.
* mm2-offsets.source.internal: This topic is used to store offsets for Kafka Connect.
* mm2-status.source.internal: This topic will store status updates of connectors and tasks.
* source.heartbeats
* source.checkpoints.internal

A typical mirror maker configuration is done via property file and defines source and target clusters with their connection properties and the replication flow definitions. Here is a simple example for a local cluster to a target cluster using TLS v1.2 for connection encryption and Sasl authentication protocol to connect to Event Streams.

```properties
clusters=source, target
source.bootstrap.servers=${KAFKA_SOURCE_BROKERS}
target.bootstrap.servers=${KAFKA_TARGET_BROKERS}
target.security.protocol=SASL_SSL
target.ssl.protocol=TLSv1.2
target.ssl.endpoint.identification.algorithm=https
target.sasl.mechanism=PLAIN
target.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="token" password=${KAFKA_TARGET_APIKEY};
# enable and configure individual replication flows
source->target.enabled=true
source->target.topics=products
tasks.max=10
```

* White listed topics are set with the `source->target.topics` attribute of the replication flow and uses [Java regular expression](https://www.vogella.com/tutorials/JavaRegularExpressions/article.html) syntax.
* Blacklisted topics: by default the following pattern is applied:

```properties
blacklist = [follower\.replication\.throttled\.replicas, leader\.replication\.throttled\.replicas, message\.timestamp\.difference\.max\.ms, message\.timestamp\.type, unclean\.leader\.election\.enable, min\.insync\.replicas]
```

But we can also define the samethings with the properties: `topics.blacklist`. Comma-separated lists are also supported and Java regular expression.

Internally `MirrorSourceConnector` and `MirrorCheckpointConnector` will create multiple tasks (up to `tasks.max` property), `MirrorHeartbeatConnector`
creates only one single task. `MirrorSourceConnector` will have one task per topic-partition to replicate, while `MirrorCheckpointConnector` will have one task per consumer group. The Kafka connect framework uses the coordinator API, with the `assign()` API, so there is no consumer group while fetching data from source topic. There is no call to `commit()` neither: the rebalancing occurs only when there is a new topic created that matches the whitelist pattern.

## Requirements to address

### Environments

We propose two approaches to run the 'on-premise' Kafka cluster:

* [Docker compose using vanilla Kafka 2.4](#scenario-1-from-kafka-local-as-source-to-event-streams-on-cloud-as-target): To run local cluster we use docker-compose and docker. The docker compose file to start a local 3 Kafka brokers and 2 Zookeepers cluster is in `mirror-maker-2/local-cluster` folder. This compose file uses a local docker network called `kafkanet`. The docker image used for Kafka is coming from Strimzi open source project and is for the Kafka 2.4 version. We are describing how to setup this simple cluster using [Docker compose in this article](dc-local.md).
* [Kafka 2.4 cluster using Strimzi operator deployed on Openshift](#scenario-2-run-mirror-maker-2-cluster-close-to-target-cluster)

For the Event Streams on Cloud cluster, we recommend to create your own using IBM Cloud Account. The product [documentation is here](https://cloud.ibm.com/registration?target=catalog/services/event-streams).

The enviroments can be summarized in the table below:

| Environment | Source                 | Target                 | Connect |
|-------------|------------------------|------------------------|:-------:|
| 1           | Local                  | Event Streams on Cloud | Local   |
| 2           | Strimzi on OCP         | Event Streams on Cloud | OCP / Roks |
| 3           | Event Streams on Cloud | Local                  | Local   |
| 4           | Event Streams on Cloud | Strimzi on OCP         | OCP/ Roks |
| 5           | Event Streams on OCP   | Event Streams on Cloud | OCP / Roks |

### Local cluster to Event Streams on Cloud

The goal is to demonstrate the replicate data from local cluster to Event Streams on IBM Cloud running as managed service. The two scenarios and the step by step approach are presented in [this note](local-to-es.md).

We have documented the replication from Event Streams as a Service to local cluster in [this note](es-to-local.md) with two scenarios depending of the target Kafka cluster (running on OpenShift or on VM / containers).

### Provisioning Connectors (Mirror Maker 2)

This main epic is related to provisioning operation.

1. As a SRE I want to provision and deploy Mirror Maker 2 connector to existing Openshift cluster without exposing password and keys so replication can start working. This will use Kubernetes secrets for configuration parameters.

    * We are describing the MM2 provisioning in [this note](mm2-provisioning).

1. As a SRE I want to understand the CLI commands used to assess to assess how the provisioning process can be automated.

    * We do not proof how to automate the deployment, but as all deployments are done with CLI and configuration files we could [consider using Ansible](mm2-provisioning#provisioning-automation) for automation.

1. As a SRE I want to understand the server sizing for the Mirror Maker environment so that I can understand how to use leanest resources for minimal needs.

    * We talk about capacity planning in [this section](mm2-provisioning#capacity-planning) and performance tests [here](perf-tests).

*Note that, there is no specific user interface for mirror maker connector.*

### Version to version migration

1. As a SRE, I want to understand how to perform a version to version migration for the Mirror Maker 2 product so that existing streaming replication is not impacted by the upgrade.

1. As a developer I want to deploy configuration updates to modify the topic white or black lists so that newly added topics are replicated.

### Security

1. As a SRE, I want to understand how client applications authenticate to source and target clusters.

1. As a developer I want to design Mirror Maker 2 based replication solution to support different line of businesses who should not connect to topics and data not related to their business and security scope.

### Monitoring

1. As a SRE, I want to get Mirror Maker 2 metrics for Prometheus so that it fits in my current metrics processing practices.

    * The explanation to setup Prometheus metris for mirror maker 2.0 is documented [here](monitoring.md).

1. As a SRE, I want to be able to add new dashboard into Grafana to visualize the Mirror Maker 2 metrics.

1. As a SRE, I want to define rules for alert reporting and configure a Slack channel for alerting.

1. [Removed] As a SRE, I want to get the Mirror Maker 2 logs into our Splunk logging platform.

### Best Practices

1. As a developer I want to understand how Mirror Maker 2 based replication addresses the record duplication.

1. As a developer I want to design replication solution to minimize the instance of Mirror Maker or being able to scale them if I observe lag into data replication processing.

1. As a developer I want to understand what are the condition for message loss.

### Performance  tests

1. As a developer I want to understand how to measure data latency and lag in data replication.

1. As a SRE I want to understand current thoughput for the replication solution.
