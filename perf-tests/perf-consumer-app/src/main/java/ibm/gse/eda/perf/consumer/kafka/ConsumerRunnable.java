package ibm.gse.eda.perf.consumer.kafka;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import ibm.gse.eda.perf.consumer.dto.CircularLinkedList;
/**
 * Consume kafka message until the process is stopped or issue on communication with the broker
 */
public class ConsumerRunnable implements Runnable {
    private static final Logger logger = Logger.getLogger(ConsumerRunnable.class.getName());
 
    private KafkaConsumer<String, String> kafkaConsumer = null;
    private boolean running = true;
    private KafkaConfiguration kafkaConfiguration;
    protected int maxMessages;
    private CircularLinkedList<String> messages;
    private  long maxLatency = 0;
    private  long minLatency = Integer.MAX_VALUE;
    private  long lagSum = 0;
    private  int count = 0;
    private  long averageLatency = 0;
    private int id;


    public ConsumerRunnable(KafkaConfiguration config,int maxMessages,int id) {
        this.kafkaConfiguration = config;
        this.maxMessages = maxMessages;
        this.id = id;
        messages = new CircularLinkedList<String>(this.maxMessages);
    }

    private void init(){
       
        kafkaConsumer = new KafkaConsumer<>(getConfig().getConsumerProperties());
        kafkaConsumer.subscribe(Collections.singletonList(getConfig().getMainTopicName()),
            new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                }

                @Override
                public void onPartitionsAssigned(Collection<org.apache.kafka.common.TopicPartition> partitions) {
                    try {
                        logger.log(Level.WARNING, "Partitions " + partitions + " assigned, consumer seeking to end.");

                        for (TopicPartition partition : partitions) {
                            long position = kafkaConsumer.position(partition);
                            logger.log(Level.WARNING, "current Position: " + position);

                            logger.log(Level.WARNING, "Seeking to end...");
                            kafkaConsumer.seekToEnd(Arrays.asList(partition));
                            logger.log(Level.WARNING,
                                    "Seek from the current position: " + kafkaConsumer.position(partition));
                            kafkaConsumer.seek(partition, position);
                        }
                        logger.log(Level.WARNING, "Producer can now begin producing messages.");
                    } catch (final Exception e) {
                        logger.log(Level.SEVERE,"Error when assigning partitions" + e.getMessage());
                    }
                }
        });
    }

    @Override
    public void run() {
        logger.log(Level.WARNING,"Start consumer performance app listening on topic: " + getConfig().getMainTopicName());
        init();
        loop();
    }

    private void loop(){
        while(running) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(kafkaConfiguration.getPollTimeOut());
                for (ConsumerRecord<String, String> record : records) {
                    logger.log(Level.WARNING, "Consumer Record - key: " 
                            + record.key() 
                            + " timestamp: "
                            + record.timestamp()
                            + " value: " 
                            + record.value()
                            + " partition: " 
                            + record.partition() 
                            + " offset: " + record.offset() + "\n");
                    // TODO assess when to use payload timestamp to compute lag
                    Date date = new Date();
                    long now = date.getTime();
                    long difference = now - record.timestamp();
                    maxLatency = max(maxLatency, difference);
                    minLatency = min(minLatency, difference);
                    lagSum += difference;
                    count = count +1;
                    averageLatency = lagSum / count;
                    messages.insertData((String)record.value());
                    logger.log(Level.WARNING,"latency: " + Long.toString(difference) + " average:" + Long.toString(averageLatency));
                }
                if (! records.isEmpty() && getConfig().getCommit()) {
                    logger.log(Level.WARNING, "Consumer auto commit");
                    kafkaConsumer.commitSync();
                }
                logger.log(Level.INFO, "in consumer: " + id + " listen to topic " + getConfig().getMainTopicName());
            } catch (final Exception e) {
                logger.log(Level.SEVERE, "Consumer loop has been unexpectedly interrupted " + e.getMessage());
                stop();
            }
     
        } //loop
    }

    public void stop(){
        logger.log(Level.WARNING, "Stop consumer");
        running = false;
    }

    public void reStart(){
        logger.log(Level.WARNING,"ReStart consumer");
        running = true;
        loop();
    }

    public boolean isRunning(){
        return running;
    }

    public KafkaConfiguration getConfig(){
        return kafkaConfiguration;
    }

	public CircularLinkedList<String> getMessages() {
		return messages;
	}

    public synchronized long getAverageLatency() {
		return this.averageLatency;
    }
    
    public synchronized long getMinLatency() {
		return this.minLatency;
	}

    public synchronized long getMaxLatency() {
		return this.maxLatency;
	}

    public synchronized long getCount(){
        return this.count;
    }

}