# Simple Kafka Consumer App 

## Implementation Approach

The approach is to implement producer and consumer to get the different timestamps to measure the latency of the data mirroring. 

![](docs/mm2-ts-test.png)

* ts-1: timestamp when creating the record object before sending
* ts-2: record timestamp when broker write to topic-partition: source topic
* ts-3: record timestamp when broker write to topic-partition: target topic
* ts-4: timestamp when polling the record

The consumer needs to be deployable on OpenShift to scale horizontally. The metrics can be exposed as metrics for Prometheus. The metrics are: average latency, min and max latencies.

The producer tool is just an simple java class and is not yet deployable as webapp.


## Building the consumer

```
mvn install
```

Your `pom.xml` file is already configured to add the consumer application to the OpenLiberty `defaultServer`. 

The `deploy` goal copies the application into the specified directory of the specified server.
In this case, the goal copies the `PerfConsumerApp.war` file into the `apps` directory of the `defaultServer` server.

## Building the docker image

To build your image, make sure that your Docker daemon is running and execute the Docker `build` command
from the command line. If you execute your build from the same directory as your Dockerfile, you can
use the period character (`.`) notation to specify the location for the build context. Otherwise, use
the `-f` flag to point to your Dockerfile:

```
docker build -t ibmcase/perfconsumerapp .
```

The first build usually takes much longer to complete than subsequent builds because Docker needs to
download all dependencies that your image requires, including the parent image.


## Running your application in Docker container

Now that your image is built, execute the Docker `run` command with the absolute path to this guide:

```
docker run -d --name perfconsumerapp -p 9080:9080 -p 9443:9443 -v $(pwd)/target/liberty/wlp/usr/servers:/servers ibmcase/perfconsumerapp
```

## Testing the container

Before you access your application from the browser, run the `docker ps` command from the command line to make sure that your container is running and didn't crash:

```
$ docker ps
CONTAINER ID        IMAGE               CREATED             STATUS              NAMES
2720cea71700        ibmcase/perfconsumerapp          2 seconds ago       Up 1 second   perfconsumerapp
```

To view a full list of all available containers, run the `docker ps -a` command from the command line.

### Access to the APIs


Here is a quick view of the openapi: http://localhost:9081/openapi/ui
![](docs/perf-api.png)

Get the metrics via API

```
http://localhost:9081/perf-consumer-app/perf/current

{ "Max-latency":51830729","Min-latency":51450505", "Average-latency":51698170}
```

Get application metrics like min, max, average latencies via the metrics: metrics/application

```
```

Get the configuration: http://localhost:9081/perf-consumer-app/config

```json
{ 
"key.deserializer": "org.apache.kafka.common.serialization.StringDeserializer","value.deserializer": "org.apache.kafka.common.serialization.StringDeserializer","enable.auto.commit": "false","group.id": "test-cons-group","bootstrap.servers": "localhost:29092,localhost:29093,localhost:29094","auto.offset.reset": "earliest","client.id": "test-cons-group-client-8980675c-fdc6-4991-95e0-793e4d487ce1"
}
```