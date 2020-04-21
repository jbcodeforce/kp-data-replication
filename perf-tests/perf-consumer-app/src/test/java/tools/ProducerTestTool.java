package tools;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.springframework.boot.SpringApplication;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import static net.sourceforge.argparse4j.impl.Arguments.store;


/**
 * The producer tool is a simple data ingestion tool that put a timestamp in the
 * payload so it can be compare to the timestamp at the consumer level, or
 * between the last topic record timestamp.
 * <p>
 * The following timestamps are interresting to measure: TS1 = the time stamp
 * when the payload is created at the producer code level TS2 = the kafka recordcd ..
 * time stamp on the first topic, in a chain of topic when doing kafka streams
 * solution for example TS3 = the kafka record time stamp on the last topic in
 * the chain TS4 = the consumer now timestamp.
 */

public class ProducerTestTool {

    public static Properties loadProperties(String bootstrap, String saslJaasConfig) {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty("sasl.mechanism", "PLAIN");
        properties.setProperty("sasl.jaas.config", saslJaasConfig);
        properties.setProperty("security.protocol", "SASL_SSL");
        properties.setProperty("ssl.protocol", "TLSv1.2");
        return properties;
    }

    public static void main(String[] args) {
        System.out.println("Start Kafka Producer Test Tool");

        try {
            ArgumentParser data = producerArgParser();
            Namespace res = data.parseKnownArgs(args, new ArrayList<>());
            String topic = res.getString("topic");
            String bootstrap = res.getString("bootstrap");
            String api = res.getString("api");
            int nb_records = res.getInt("numRecords");
            String username = "token";
            String jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
            String saslJaasConfig = String.format(jaasTemplate, username, api);

            KafkaProducer<String, String> producer = new KafkaProducer<String, String>(loadProperties(bootstrap, saslJaasConfig));
            int count = 0;
            HashMap<String, Object> map = new HashMap<>();
            for (int i = 0; i < nb_records; i++) {
                Date date = new Date();
                // add more properties to message
                map.put("timestamp", date.getTime());
                map.put("value", count);
                map.put("id", count);
                System.out.println(map);
                ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, map.toString());
                producer.send(record);
                count++;
            }
            System.out.println(count);
        } catch (ArgumentParserException e) {
            // TODO Auto-generated catch block
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
                .help("read messages from this topic");

        parser.addArgument("--bootstrap")
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("BOOTSTRAP")
                .help("bootstrap server. commad separated value");

        parser.addArgument("--api")
                .action(store())
                .required(true)
                .type(String.class)
                .metavar("API")
                .help("API key to connect to server");

        parser.addArgument("--num-records")
                .action(store())
                .required(true)
                .type(Integer.class)
                .metavar("NUM-RECORDS")
                .dest("numRecords")
                .help("number of messages to produce");
        return parser;
    }
}