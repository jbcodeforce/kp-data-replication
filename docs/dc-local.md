# Running a Kafka Cluster with Docker Compose

We are providing a [docker compose file]() to start a local 3 Broker Kafka cluster with 2 Zookeeper nodes.

* In one Terminal window, start the local cluster using `docker-compose` under the `local-cluster` folder: `docker-compose up &`. The data are persisted on the local disk within this folder.
* If this is the first time you start this local Kafka cluster, you need to create the `products` topic. Start a Kafka container to access the Kafka tools with the command:

  ```shell
  docker run -ti -v $(pwd):/home --network kafkanet strimzi/kafka:latest-kafka-2.4.0 bash
  ```

  Then in the bash shell, go to `/home/local-cluster` folder and execute the script: `./createProductsTopic.sh`. Verify topic is created with the command: `/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka1:9092 --list`

  Your environment is up and running.
