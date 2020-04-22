package tools;

import static net.sourceforge.argparse4j.impl.Arguments.store;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * The producer tool is a simple data ingestion tool that put a timestamp in the
 * payload so it can be compare to the timestamp at the consumer level, or
 * between the last topic record timestamp.
 * <p>
 * The following timestamps are interresting to measure: TS1 = the time stamp
 * when the payload is created at the producer code level TS2 = the kafka
 * recordcd .. time stamp on the first topic, in a chain of topic when doing
 * kafka streams solution for example TS3 = the kafka record time stamp on the
 * last topic in the chain TS4 = the consumer now timestamp.
 */

public class ProducerTestTool {
    private String api = null;
    private String topic = null;
    private String bootstrapServers = null;
    private int nb_records = 10;
    private KafkaProducer<String, String> producer = null;

    public ProducerTestTool() {
    }

    public ProducerTestTool(String bss, String api, String topic, int nbr) {
        this.bootstrapServers = bss;
        this.api = api;
        this.topic = topic;
        this.nb_records = nbr;
    }

    public Properties loadProperties() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return properties;
    }

    public void prepareProducer() {
        Properties properties = loadProperties();
        if (api != null) {
            String username = "token";
            String jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
            String saslJaasConfig = String.format(jaasTemplate, username, api);
            properties.setProperty("sasl.mechanism", "PLAIN");
            properties.setProperty("sasl.jaas.config", saslJaasConfig);
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("ssl.protocol", "TLSv1.2");
        }

        producer = new KafkaProducer<String, String>(properties);
    }

    public void produce() throws JsonProcessingException, InterruptedException, ExecutionException {
        int count = 0;
        HashMap<String, Object> map = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < nb_records; i++) {
            Date date = new Date();
            Payload p = new Payload(Integer.toString(count), "Value " + i, date.getTime());
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(
                    topic, 
                    p.id,
                    mapper.writeValueAsString(p)
                    );
            System.out.println(record.toString());
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata=future.get();
            System.out.println(metadata.toString());
            System.out.println(metadata.offset());
            System.out.println(metadata.timestamp());
            count++;
        }
        System.out.println("Number of messages sent:" + count);
    }
    public static void main(String[] args) {
        System.out.println("Start Kafka Producer Test Tool");

        try {
            ArgumentParser argumentParser = producerArgParser();
            Namespace res = argumentParser.parseKnownArgs(args, new ArrayList<>());
            ProducerTestTool tool = new ProducerTestTool(
                res.getString("bootstrap"),
                res.getString("api"),
                res.getString("topic"),
                res.getInt("numRecords")
            );
            tool.prepareProducer();
            tool.produce();
        } catch (ArgumentParserException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


    }

    private static ArgumentParser producerArgParser() {

        ArgumentParser parser = ArgumentParsers
                .newArgumentParser("end-to-end-performance")
                .defaultHelp(true)
                .description("This tool is used to verify end-to-end performance of application");

        parser.addArgument("--topic")
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("TOPIC")
                .help("write messages to this topic");

        parser.addArgument("--bootstrap")
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("BOOTSTRAP")
                .help("bootstrap server. comma separated value");

        parser.addArgument("--api")
                .action(store())
                .required(false)
                .type(String.class)
                .metavar("API")
                .help("API key to connect to event stream server");

        parser.addArgument("--num-records")
                .action(store())
                .required(true)
                .type(Integer.class)
                .metavar("NUM-RECORDS")
                .dest("numRecords")
                .help("number of messages to produce");
        return parser;
    }

    public class Payload {
        public long timestamp;
        public String value;
        public String id;
        public Payload(){}
        public Payload(String id, String v, long ts) {
            this.id = id;
            this.value = v;
            this.timestamp = ts;
        };

    }
}