package ibm.gse.eda.perf.consumer.kafka;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Centralize the kafka configuration parameters. It reads the data from the
 * resources/microprofile-config.properties
 */
@ApplicationScoped
public class KafkaConfiguration {
    private static final Logger logger = Logger.getLogger(KafkaConfiguration.class.getName());
    public static final Duration CONSUMER_POLL_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration CONSUMER_CLOSE_TIMEOUT = Duration.ofSeconds(10);
    public static final long TERMINATION_TIMEOUT_SEC = 10;
    private static final KafkaConfiguration instance = new KafkaConfiguration();
    private Properties properties = new Properties();
    @Inject
    @ConfigProperty(name = "kafka.topic.name", defaultValue = "accounts")
    protected String mainTopicName;


    @Inject
    @ConfigProperty(name = "kafka.groupid", defaultValue = "accounts-group")
    private String groupId;

    @Inject
    @ConfigProperty(name = "kafka.consumer.commit", defaultValue = "false")
    private boolean commit;

    @Inject
    @ConfigProperty(name = "kafka.consumer.offsetPolicy", defaultValue = "earliest")
    private String offsetPolicy;

    @Inject
    @ConfigProperty(name = "kafka.brokers", defaultValue = "localhost:9092")
    private String brokers;

    @Inject
    @ConfigProperty(name = "kafka.poll.timeout")
    private String pollTimeout = null;

    @Inject
    @ConfigProperty(name = "kafka.poll.records")
    private String pollRecords = null;

    @Inject
    @ConfigProperty(name="kafka.key.deserializer", defaultValue="org.apache.kafka.common.serialization.ByteArrayDeserializer")
    private String keyDeserializer;

    @Inject
    @ConfigProperty(name="kafka.value.deserializer", defaultValue="org.apache.kafka.common.serialization.ByteArrayDeserializer")
    private String valueDeserializer;

    public KafkaConfiguration() {
    }

    public static KafkaConfiguration getInstance() {
        return instance;
    }

    /**
     * Take into account the environment variables
     *
     * @return common kafka properties
     */
    private Properties buildCommonProperties() {
       
        Map<String, String> env = System.getenv();
        if (env.containsKey("KAFKA_BROKERS")) {
            setBrokers(env.get("KAFKA_BROKERS"));
        } 
        properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getBrokers());

        if (env.get("KAFKA_APIKEY") != null && env.get("KAFKA_APIKEY").length() > 0) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
            properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
            properties.put(SaslConfigs.SASL_JAAS_CONFIG,
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"token\" password=\""
                            + env.get("KAFKA_APIKEY") + "\";");
            properties.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
            properties.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
            properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "HTTPS");
        }
        if (env.get("TRUSTSTORE_PATH") != null && env.get("TRUSTSTORE_PATH").length() > 0) {
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            properties.setProperty(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
            properties.setProperty(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
            properties.setProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "HTTPS");
            properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, env.get("TRUSTSTORE_PATH"));
            properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.get("TRUSTSTORE_PWD"));
        }
        return properties;
    }

    public Properties getConsumerProperties(String groupID, boolean commit, String offset) {
        buildCommonProperties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupID);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(commit));
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, groupID + "-client-" + UUID.randomUUID());
        properties.forEach((k, v) -> logger.warning(k + " : " + v));
        return properties;
    }

    public Properties getConsumerProperties() {
       return  getConsumerProperties(getGroupId(),getCommit(),getOffsetPolicy());
    }


    public void setGroupId(String v) {
        this.groupId = v;
    };

    public String getGroupId() {
        return this.groupId;
    }

    public String getMainTopicName() {
        return this.mainTopicName;
    }

    public void setMainTopicName(String mainTopicName) {
        this.mainTopicName = mainTopicName;
    };
 
    public boolean getCommit(){
        return this.commit;
    }

    public void setCommit(boolean b) {
        this.commit = b;
    }

    public String getOffsetPolicy(){
        return this.offsetPolicy;
    }

    public void setOffsetPolicy(String p) {
        this.offsetPolicy = p;
    }

    public String getBrokers(){
        return this.brokers;
    }

    public void setBrokers(String n){
        this.brokers = n;
    }

    public Duration getPollTimeOut(){
        if (this.pollTimeout == null) {
            return CONSUMER_POLL_TIMEOUT;
        }
        return Duration.ofMillis(Long.parseLong(pollTimeout));
    }

    public String getPropertiesAsString(){
        StringBuffer sb = new StringBuffer();
        sb.append("{ \n");
        properties.forEach((k, v) -> {
            if (! k.equals(SaslConfigs.SASL_JAAS_CONFIG)) {sb.append("\"" + k + "\": \"" + v +"\",");}
            });
        sb.append("\"topic\": \"");
        sb.append(this.getMainTopicName());
        sb.append("\"}");
        return sb.toString();
    }
}
