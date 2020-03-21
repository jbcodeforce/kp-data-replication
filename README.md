# KP Data Replication

This repository includes a set of documents for best practices around data replication between two Kafka clusters. 

Better to read in [BOOK format](https://jbcodeforce.github.io/kp-data-replication).

## Requirements

### Mirror Maker 2.0

#### Local cluster to Event Streams on Cloud

The goal is to replicate data from local cluster to Event Streams on IBM Cloud as managed service. The two scenarios are presented in [this note](#local-to-es).

* Local Kafka Cluster (kafka on laptop or Openshift cluster) to Event Streams (Public) using mirror maker 2.0 on cloud on openshift

#### Provisioning Connectors (Mirror Maker 2)

* How to handle secrets (ES credentials, CK keys and certificates).
* GUI tools
* Command line / REST API

#### Version to version migration

How to address Roll out upgrades to MM2 cluster

### Security

* Topic segregation (multi-tenant Kafka Connect cluster)
* ACLs

### Monitoring

* Prometheus metrics (On same setup as CK monitoring)
* Dashboards
* Alerts
* Log formats, and logs going into Splunk

### Best Practices

* Correct configuration
* How to address scenarios with duplicates (no Exactly Once)?
* How many MM connectors per cluster?
* How many nodes per cluster?
* How do we scale the MM2 cluster?
* Other best practices

### Performance  tests

* How to measure latency (avg. time per message end-to-end)
* Ensure message affinity
* Message loss
* Duplicates (No Exactly Once)