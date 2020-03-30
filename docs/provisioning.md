# Strimzi Operator and Kafka Cluster Provisioning

In this note we propose to describe the provisioning of a Kafka Cluster using Strimzi operators and how to provision Mirror Maker 2 on Kubernetes or on VM.

[Strimzi](https://strimzi.io/) uses the Cluster Operator to deploy and manage Kafka (including Zookeeper) and Kafka Connect clusters. When the Strimzi Cluster Operator is up, it starts to watch for certain OpenShift or Kubernetes resources containing the desired Kafka and/or Kafka Connect cluster configuration. The base of strimzi is to define a set of kubernetes operators and custom resource definitions for the different elements of Kafka.

![Strimzi](images/strimzi.png)

We recommend to go over the product [overview page](https://strimzi.io/docs/overview/master/).

*The service account and role binding do not need to be re-installed if you did it previously.*

## Concept summary

The Cluster Operator is a pod used to deploys and manages Apache Kafka clusters, Kafka Connect, Kafka MirrorMaker (1 and 2), Kafka Bridge, Kafka Exporter, and the Entity Operator. When deployed the following commands goes to the Cluster operator:

```shell
# Get the current cluster list
oc get kafka
# get the list of topic
oc get kafkatopics
```

Example of topic can be seen in [section below](#create-a-topic).

Kafka User are not saved part of kafka cluster but they are managed in kubernetes. For example the user credentials are saved as secret.

CRDs act as configuration instructions to describe the custom resources in a Kubernetes cluster, and are provided with Strimzi for each Kafka component used in a deployment.

## Strimzi Operators Deployment

The deployment is done in two phases:

* Deploy the Custom Resource Definitions (CRDs), which act as specifications of the custom resource to deploy.
* Deploy one to many instance of those CRDs

In CR yaml file the `kind` attribute specifies the CRD to conform to.

Each CRD has a common configuration like bootstrap servers, CPU resources, logging, healthchecks...

The next steps are defining how to deploy a Kafka Cluster.

### Create a namespace or openshift project

```shell
kubectl create namespace eda-strimzi-kafka24 
# Or using Openshift CLI
oc new-project eda-strimzi-kafka24 
```

### Download the strimzi artefacts

We have already created the configuration from the source strimzi github in the following folder `openshift-strimzi/eda-strimzi-kafka24`. So you do not need to do the following steps if you use the project: `eda-strimzi-kafka24`.

In case you want to do on your own, get the last Strimzi release from [this github page](https://github.com/strimzi/strimzi-kafka-operator/releases). Then modify the Role binding yaml files with the namespace set in previous step.

```shell
sed -i '' 's/namespace: .*/namespace: eda-strimzi-kafka24 /' $strimzi-home/install/cluster-operator/*RoleBinding*.yaml
```

### Deploy the Custom Resource Definitions for kafka

Custom resource definitions are defined within the kubernetes cluster. The following commands  

```shell
oc apply -f openshift-strimzi/eda-strimzi-kafka24/cluster-operator/
oc get crd
```

In case of Strimzi cluster operator fails with error like: " kafkas.kafka.strimzi.io is forbidden: User "system:serviceaccount:eda-strimzi-kafka24 :strimzi-cluster-operator" cannot watch resource "kafkas" in API group "kafka.strimzi.io" in the namespace "eda-strimzi-kafka24 ", you need to add cluster role to the strimzi operator user by doing the following commands:

```shell
oc adm policy add-cluster-role-to-user strimzi-cluster-operator-namespaced --serviceaccount strimzi-cluster-operator -n eda-strimzi-kafka24
oc adm policy add-cluster-role-to-user strimzi-entity-operator --serviceaccount strimzi-cluster-operator -n eda-strimzi-kafka24
```

The commands above, should create the following service account, resource definitions, roles, and role bindings:

| Names | Resource | Command |
| --- | :---: | --- |
| strimzi-cluster-operator | Service account | oc get sa |
| strimzi-cluster-operator-entity-operator-delegation, strimzi-cluster-operator, strimzi-cluster-operator-topic-operator-delegation | Role binding | oc get rolebinding |
| strimzi-cluster-operator-global, strimzi-cluster-operator-namespaced, strimzi-entity-operator, strimzi-kafka-broker, strimzi-topic-operator | Cluster Role | oc get clusterrole |
| strimzi-cluster-operator, strimzi-cluster-operator-kafka-broker-delegation | Cluster Role Binding | oc get clusterrolebinding |
| kafkabridges, kafkaconnectors, kafkaconnects, kafkamirrormaker2s kafka, kafkatopics, kafkausers | Custom Resource Definitions | oc get customresourcedefinition |grep kafka|

### Add Strimzi Admin Role

If you want to allow non-kubernetes cluster administators to manage Strimzi resources, you must assign them the Strimzi Administrator role.

First deploy the role definition using the folowing command: `oc apply -f openshift-strimzi/eda-strimzi-kafka24/010-ClusterRole-strimzi-admin.yaml`

Then assign the strimzi-admin ClusterRole to one or more existing users in the Kubernetes cluster.

`kubectl create clusterrolebinding strimzi-admin --clusterrole=strimzi-admin --user=<user-your-username-here>`

## Deploy instances

### Deploy Kafka cluster

The CRD for kafka cluster resource is [here](https://github.com/strimzi/strimzi-kafka-operator/blob/2d35bfcd99295bef8ee98de9d8b3c86cb33e5842/install/cluster-operator/040-Crd-kafka.yaml) and we recommend to study it before defining your own cluster.

Change the name of the cluster in one the yaml in the `examples/kafka` folder or use the `openshift-strimzi/kafka-cluster.yml` file in this project. This file defines the default replication factor of 3 and in-synch replicas of 2. For development purpose we will accept plain (unencrypted) listener on port 9092 without TLS authentication.
For external to the kubernetes cluster access we need to have external listeners. For Openshift, as we use routes, we need to add the `external.type = route`. When exposing Kafka using OpenShift Routes and the HAProxy router, a dedicated Route is created for every Kafka broker pod. An additional Route is created to serve as a Kafka bootstrap address. Kafka clients can use these Routes to connect to Kafka on port 443.

Even for development we added the metrics rules to expose kafka and zookeeper metrics for tool like Prometheus.

For production we need to use persistence for the kafka log, ingress or load balancer external listener and rack awareness policies. It has to use Mutual TLS authentication, and with Strimzi we can use the User Operator to manage cluster users. Mutual authentication or two-way authentication is when both the server and the client present certificates.

Using non presistence:

```shell
oc apply -f openshift-strimzi/kafka-cluster.yaml
oc get kafka
# NAME         DESIRED KAFKA REPLICAS   DESIRED ZK REPLICAS
# my-cluster   3                        3
```

When looking at the pods running we can see the three kafka and zookeeper nodes as pods, and the entity operator pod.

```shell
$ oc get pods
my-cluster-entity-operator-645fdbc4cb-m29nk   3/3       Running     0          18d
my-cluster-kafka-0                            2/2       Running     0          3d
my-cluster-kafka-1                            2/2       Running     0          3d
my-cluster-kafka-2                            2/2       Running     0          3d
my-cluster-zookeeper-0                        2/2       Running     0          3d
my-cluster-zookeeper-1                        2/2       Running     0          3d
my-cluster-zookeeper-2                        2/2       Running     0          3d
strimzi-cluster-operator-58cbbcb7d-bcqhm      1/1       Running     2         18d
strimzi-topic-operator-564654cb86-nbt58       1/1       Running     1         18d
```

To use persistence add persistence volume and declare the PVC in the yaml file and then reapply:

```shell
oc apply -f strimzi/kafka-cluster.yaml
```

## Add Topic CRDs and operator

This step is optional. Topic operator helps to manage Kafka topics via yaml configuration and get map the topics as kubernetes resources so a command like `oc get kafkatopics` returns the list of topics. The operator keep the resources and the kafka topic in synch. This allows you to declare a KafkaTopic as part of your application’s deployment.

To manage Kafka topics with operators, first modify the file `05-Deployment-strimzi-topic-operator.yaml` to reflect your cluster name

```yaml
env:
            - name: STRIMZI_RESOURCE_LABELS
              value: "strimzi.io/cluster=eda-demo-24-cluster"
            - name: STRIMZI_KAFKA_BOOTSTRAP_SERVERS
              value: eda-demo-24-cluster-kafka-bootstrap:9092
            - name: STRIMZI_ZOOKEEPER_CONNECT
              value: eda-demo-24-cluster-zookeeper-client:2181
```

and then deploy the topic-operator. This operation will fail if there is no Kafka Boker and Zookeeper available:

```shell
oc apply -f openshift-strimzi/install/topic-operator
oc adm policy add-cluster-role-to-user strimzi-topic-operator --serviceaccount strimzi-cluster-operator -n eda-strimzi-kafka24 
```

This will add the following:

| Names | Resource | Command |
| :---: | :---: | :---: |
| strimzi-topic-operator | Service account | oc get sa |
| strimzi-topic-operator| Role binding | oc get rolebinding |
| kafkatopics | Custom Resource Definition | oc get customresourcedefinition |

### Create a topic

Edit a yaml file like the following:

```yaml
apiVersion: kafka.strimzi.io/v1beta1
kind: KafkaTopic
metadata:
  name: test
  labels:
    strimzi.io/cluster: eda-demo-24-cluster
spec:
  partitions: 1
  replicas: 3
  config:
    retention.ms: 7200000
    segment.bytes: 1073741824
```

```shell
oc apply -f test.yaml

oc get kafkatopics
```

This creates a topic `test` in your kafka cluster.

## Add User CRDs and operator

This step is optional. To manage Kafka user with operators modify the file `05-Deployment-strimzi-user-operator.yaml` to reflect your cluster name and then deploy the user-operator:

```shell
oc apply -f openshift-strimzi/install/user-operator
```

## Test with producer and consumer pods

Use kafka-consumer and producer tools from Kafka distribution. Verify within Dockerhub under the Strimzi account to get the lastest image tag (below we use -2.4.0 tag).

```shell
# Start a consumer on test topic

oc run kafka-consumer -ti --image=strimzi/kafka:latest-kafka-2.4.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic test --from-beginning
# Start a text producer
oc run kafka-producer -ti --image=strimzi/kafka:latest-kafka-2.4.0  --rm=true --restart=Never -- bin/kafka-console-producer.sh --broker-list my-cluster-kafka-bootstrap:9092 --topic test
# enter text
```

If you want to use the strimzi kafka docker image to run the above scripts on your local computer, remotely connect to a kafka cluster you need multiple things to happen:

* Be sure the kafka custer definition yaml file includes the `external` route stamza:

```yaml
spec:
  kafka:
    version: 2.4.0
    replicas: 3
    listeners:
      plain: {}
      tls: {}
      external:
        type: route
```

* Get the host ip address from the Route resource

```shell
oc get routes my-cluster-kafka-bootstrap -o=jsonpath='{.status.ingress[0].host}{"\n"}'
```

* Get the TLS certificate from the broker

```shell
oc get secrets
oc extract secret/eda-demo-24-cluster-cluster-ca-cert --keys=ca.crt --to=- > ca.crt
# transform it fo java truststore
keytool -import -trustcacerts -alias root -file ca.crt -keystore truststore.jks -storepass password -noprompt
```

*The alias is used to access keystore entries (key and trusted certificate entries).*

* Start the docker container by mounting the local folder with the truststore.jks to the `/home`

```shell
docker run -ti -v $(pwd):/home strimzi/kafka:latest-kafka-2.4.0  bash
# inside the container uses the consumer tool
bash-4.2$ cd /opt/kafka/bin
bash-4.2$ ./kafka-console-consumer.sh --bootstrap-server  eda-demo-24-cluster-kafka-bootstrap-eda-strimzi-kafka24 .gse-eda-demos-fa9ee67c9ab6a7791435450358e564cc-0001.us-east.containers.appdomain.cloud:443 --consumer-property security.protocol=SSL --consumer-property ssl.truststore.password=password --consumer-property ssl.truststore.location=/home/truststore.jks --topic test --from-beginning
```

* For a producer the approach is the same but using the producer properties:

```shell
./kafka-console-producer.sh --broker-list  my-cluster-kafka-bootstrap-eda-strimzi-kafka24 .gse-eda-demos-fa9ee67c9ab6a7791435450358e564cc-0001.us-east.containers.appdomain.cloud:443 --producer-property security.protocol=SSL --producer-property ssl.truststore.password=password --producer-property ssl.truststore.location=/home/truststore.jks --topic test
```

Those properties can be in file

```shell
bootstrap.servers=eda-demo-24-cluster-kafka-bootstrap-eda-strimzi-kafka24 .gse-eda-demos-fa9ee67c9ab6a7791435450358e564cc-0001.us-east.containers.appdomain.cloud
security.protocol=SSL
ssl.truststore.password=password
ssl.truststore.location=/home/truststore.jks
```

and then use the following parameters in the command line:

```shell
./kafka-console-producer.sh --broker-list eda-demo-24-cluster-kafka-bootstrap-eda-strimzi-kafka24 .gse-eda-demos-fa9ee67c9ab6a7791435450358e564cc-0001.us-east.containers.appdomain.cloud:443 --producer.config /home/strimzi.properties --topic test

./kafka-console-consumer.sh --bootstrap-server eda-demo-24-cluster-kafka-bootstrap-eda-strimzi-kafka24 .gse-eda-demos-fa9ee67c9ab6a7791435450358e564cc-0001.us-east.containers.appdomain.cloud:443  --topic test  --consumer.config /home/strimzi.properties --from-beginning
```
