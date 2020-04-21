package tools;

import java.util.Properties;

/**
 * The producer tool is a simple data ingestion tool that put a timestamp in the
 * payload so it can be compare to the timestamp at the consumer level, or
 * between the last topic record timestamp.
 * 
 * The following timestamps are interresting to measure: TS1 = the time stamp
 * when the payload is created at the producer code level TS2 = the kafka record
 * time stamp on the first topic, in a chain of topic when doing kafka streams
 * solution for example TS3 = the kafka record time stamp on the last topic in
 * the chain TS4 = the consumer now timestamp.
 */
public class ProducerTestTool {

 public static Properties loadProperties(){
     return null;
 }
 public static void main(String[] args) {
     System.out.println("Start Kafka Consumer Test Tool");

 }
}