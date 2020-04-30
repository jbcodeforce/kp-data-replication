package ibm.gse.eda.perf.consumer.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import ibm.gse.eda.perf.consumer.app.dto.Control;

/**
 * Controller for kafka consumers.
 * Per default it starts a number of executor equals to the number of partition set on the topic
 */
@ApplicationScoped
public class PerfConsumerController {
    private static final Logger logger = Logger.getLogger(PerfConsumerController.class.getName());

    private ExecutorService executor;
    private ArrayList<ConsumerRunnable> consumers = new ArrayList<ConsumerRunnable>();
    @Inject
    private KafkaConfiguration kafkaConfiguration;
    @Inject
    @ConfigProperty(name = "app.maxMessages", defaultValue = "100")
    protected int maxMessages;
    @Inject
    @ConfigProperty(name = "app.maxConsumers", defaultValue = "10")
    protected int maxConsumers = 10;
    @Inject
    @ConfigProperty(name = "kafka.topic.numberOfPartitions", defaultValue = "1")
    protected int numberOfPartitions;


    public PerfConsumerController() {  
        logger.warning("Controller constructor " + this.toString());
    }
   
    public PerfConsumerController(int maxMsg, int numberOfPartitions, KafkaConfiguration cfg) {
        this.numberOfPartitions = numberOfPartitions;
        this.maxMessages = maxMsg;
        kafkaConfiguration = cfg;
        prepareAndStartConsumers();
    }

    @PostConstruct
    public void prepareAndStartConsumers(){
        logger.warning("Controller prepareAndStartConsumers " + this.toString());
        executor = Executors.newFixedThreadPool(maxConsumers);
        for (int i = 0; i< numberOfPartitions; i++) {
            ConsumerRunnable runnable = new ConsumerRunnable(kafkaConfiguration,maxMessages,i);
            consumers.add(runnable);
            executor.execute(runnable);
        }
    }

    public void adaptNumberOfConsumersAndStart(int newNumberOfPartitions){
        if (newNumberOfPartitions > maxConsumers) {
            newNumberOfPartitions = maxConsumers;
        }
        if (newNumberOfPartitions > numberOfPartitions) {
            for (int i = numberOfPartitions; i < newNumberOfPartitions;i++) {
                ConsumerRunnable runnable = new ConsumerRunnable(kafkaConfiguration,maxMessages,i);
                consumers.add(runnable);
                executor.execute(runnable);
            }
        }

    }

    public List<String> getMessages(){
        ArrayList<String> l = new ArrayList<String>(maxMessages);
        for (ConsumerRunnable consumerRunnable : consumers) {
          l.addAll(consumerRunnable.getMessages().getElements());
        }
        for (String c : l) {
            System.out.println(c);
        }
        return l;
    }

     public long getAverageLatencyCrossConsumers(){
        long averageLatency = 0;
        for (ConsumerRunnable consumerRunnable : consumers) {
            averageLatency += consumerRunnable.getAverageLatency();
        }
        return averageLatency;
     }

     public long getMaxLatencyCrossConsumers(){
         long maxLatency = Long.MIN_VALUE;
         for (ConsumerRunnable consumerRunnable : consumers) {
            maxLatency = Long.max(maxLatency, consumerRunnable.getMaxLatency());
         }
         return maxLatency;
     }

     public long getMinLatencyCrossConsumers(){
        long minLatency = Long.MAX_VALUE;
         for (ConsumerRunnable consumerRunnable : consumers) {
             minLatency = Long.min(minLatency, consumerRunnable.getMinLatency());
         }
         return minLatency;
     }
     
     public long getCountCrossConsumers(){
        long count = Long.MIN_VALUE;
        for (ConsumerRunnable consumerRunnable : consumers) {
            count += consumerRunnable.getCount();
        }
        return count;
     }
    /**
     * Change the configuration of the consumers.
     * @param control
     */
     public boolean controlConsumers(Control control){
         if ("STOP".equals(control.order)) {
            stopConsumers();
            return true;
         }

         if ("RESTART".equals(control.order)) {
            stopConsumers();
            cleanConsumers();
            redefineConfiguration(control);
            prepareAndStartConsumers();
           return true;
         }
         if ("UPDATE".equals(control.order)) {
            adaptNumberOfConsumersAndStart(control.numberOfPartitions);
            return true;
         }
         return false;
     }

    public void stopConsumers(){
        for (ConsumerRunnable consumerRunnable : consumers) {
            if (consumerRunnable.isRunning()) {
                consumerRunnable.stop();
            }   
        }
        executor.shutdownNow();
        try {
            executor.awaitTermination(KafkaConfiguration.TERMINATION_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            logger.warning("await Termination( interrupted " + ie.getLocalizedMessage());
        }
    }

    private void cleanConsumers(){
        consumers = new ArrayList<ConsumerRunnable>();
    }

    private void redefineConfiguration(Control ctl) {
        this.kafkaConfiguration.setMainTopicName(ctl.topic);
        this.kafkaConfiguration.setCommit(ctl.commit);
        this.kafkaConfiguration.setOffsetPolicy(ctl.offsetPolicy);
        this.numberOfPartitions = ctl.numberOfPartitions;
    }

	public int getNumberOfConsumers() {
		return consumers.size();
	}

	public int getNumberOfRunningConsumers() {
        int i = 0;
        for (ConsumerRunnable consumerRunnable : consumers) {
            if (consumerRunnable.isRunning()) {
                i++;
            }
        }
		return i;
    }
    
    public int getMaxConsumers(){
        return this.maxConsumers;
    }

	public boolean isFullyRunning() {
		return (getNumberOfRunningConsumers() == maxConsumers);
    }
    
    public int getNumberOfPartitions() {
        return this.numberOfPartitions;
    }

    public void setNumberOfPartitions(int numberOfPartitions) {
        this.numberOfPartitions = numberOfPartitions;
    };
}
