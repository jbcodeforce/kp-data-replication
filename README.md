# KP Data Replication

This repository includes a set of documents for best practices around data replication between two Kafka clusters. 

Better to read in [BOOK format](https://jbcodeforce.github.io/kp-data-replication).

## Requirements

### Mirror Maker 2.0

#### Local cluster to Event Streams on Cloud

The goal is to demonstrate the replicate data from local cluster to Event Streams on IBM Cloud running as managed service. The two scenarios and the step by step approach are presented in [this note](https://jbcodeforce.github.io/kp-data-replication/local-to-es).

We have also documented the replication approaches from Event Streams as a Service to local cluster in [this note](https://jbcodeforce.github.io/kp-data-replication/es-to-local).

#### Provisioning Connectors (Mirror Maker 2)

This main epic is related to provisioning operation. As a SRE I want to provision and deploy Mirror Maker 2 connector to existing Openshift cluster without exposing password and keys so replication can start working. This will use Kubernetes secrets for configuration parameters. 

As a SRE I want to understand the CLI commands used to assess how automation can be performed for replicating environment provisioning.

As a SRE I want to understand the server sizing for the Mirror Maker environment.

*Note that, there is no specific user interface for mirror maker connector.*

#### Version to version migration

As a SRE, I want to understand how to perform a version to version migration for the Mirror Maker 2 product so that existing streaming replication is not impacted by the upgrade.

As a developer I want to deploy configuration updates to modify the topic white or black lists so that newly added topics are replicated.

### Security

As a SRE, I want to understand how the security support to connect client applications to cluster and to replicated topic.

As a developer I want to design Mirror Maker 2 based replication solution to support different line of businesses who should not connect to topics and data not related to their business and security scope.

### Monitoring

As a SRE, I want to get Mirror Maker 2 metrics for Prometheus so that it fits in my current metrics processing practices.

As a SRE, I want to be able to add new dashboard into Grafana to visualize the Mirror Maker 2 metrics.

As a SRE, I want to define rules for alert reporting and configure a Slack channel for alerting.

As a SRE, I want to get the Mirror Maker 2 logs into our Splunk logging platform.

### Best Practices

As a developer I want to understand how Mirror Maker 2 based replication address the record duplication.

As a developer I want to design replication solution to minimize the instance of Mirror Maker or being able to scale them if I observe lag into data replication processing.

As a developer I want to understand what are the condition for message loss.

### Performance  tests

As a developer I want to understand how to measure data latency and lag in data replication.

As a SRE I want to understand current thoughput for the replication solution.

* Ensure message affinity
