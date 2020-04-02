# Performance tests

The mirroring performance test is based on the following architecture: 

![](images/perf-test-env.png)

A producer code based on the [Kafka producer performance tool](https://github.com/apache/kafka/blob/trunk/tools/src/main/java/org/apache/kafka/tools/ProducerPerformance.java) which can read data from a file and generate n records to the source topic. It reports test metrics like records per second, number of records sent, the megabytes per second, the average and maximum latencies,  from the producer.metrics() and other stats from the tools.

This tool can also use transaction. The arguments supported are:

| Argument | Description|
| --- | --- |
| --record-size `value`> or --payload-file `filename` | one is mandatory but not both. Work only for UTF-8 encoded text files|
| --topic `name` | mandatory |
| --num-records `value` | mandatory |
| --payload-delimiter `char` | delimiter to be used when --payload-file is provided. Default is \n |
| --throughput `value` | Throttle to value messages/sec. -1 to disable throtlling|
| --producer.config `filename` | producer config properties file|
| --producer-props `prop_name=value` | producer configuration properties|
| --print-metrics `true|false`| Default to true|
| --transactional-id `value` | Test with transaction |
| --transaction-duration-ms `value`| The max age of each transaction. Test with transaction if v>0 |


IBM Event Stream provides a wrapper on top of this tool to communicate with Event streams. [This article](https://ibm.github.io/event-streams/getting-started/testing-loads/) explains how to use the tool.

## Test approach

Using IBM event streams producer tool we can run 3 different workload size, the payload is generated with random bytes or with records read from a data file.

```shell
java -jar target/es-producer.jar -t test -s small
java -jar target/es-producer.jar -t test -s medium
java -jar target/es-producer.jar -t test -s large
```

To build the jar, we have cloned the repository under the [event-streams-sample-producer-1.1.0](https://github.com/jbcodeforce/kp-data-replication/tree/master/perf-tests/event-streams-sample-producer-1.1.0) and build the jar with `mvn package`.

Here is an example of call to this performance producer

```shell
 java -jar event-streams-sample-producer-1.1.0/target/es-producer.jar --payload-file ./da/records.json -t topic --producer-config ../mirror-maker-2/eventstream.properties
 ```

## Measurements

This is the type of traces reported.

```shell
1261 records sent, 251.8 records/sec (0.97 MB/sec), 2233.3 ms avg latency, 3320.0 max latency.
2848 records sent, 569.5 records/sec (2.19 MB/sec), 5797.0 ms avg latency, 8294.0 max latency.
2840 records sent, 567.8 records/sec (2.19 MB/sec), 10760.9 ms avg latency, 13288.0 max latency.

..

60000 records sent, 490.869821 records/sec (1.89 MB/sec), 14698.93 ms avg latency, 24949.00 ms max latency, 14490 ms 50th, 22298 ms 95th, 23413 ms 99th, 24805 ms 99.9th.
```

Adding the consumer is simple, start a python environment using docker image

```shell
docker run -e KAFKA_BROKERS=$KAFKA_BROKERS --rm -v $(pwd):/home -it  ibmcase/python37 bash
$ python consumer/PerfConsumer.py
```

