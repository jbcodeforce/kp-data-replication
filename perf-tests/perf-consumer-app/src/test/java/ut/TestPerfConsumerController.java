package ut;

import org.junit.Assert;
import org.junit.Test;

import ibm.gse.eda.perf.consumer.kafka.KafkaConfiguration;
import ibm.gse.eda.perf.consumer.kafka.PerfConsumerController;

public class TestPerfConsumerController {

    @Test
    public void shouldHaveOneConsumerReady(){
        KafkaConfiguration cfg = new KafkaConfiguration();
        // 20 msgs, 1 partition on topic
        PerfConsumerController controller = new PerfConsumerController(20,1,cfg);
        Assert.assertNotNull(controller);
        Assert.assertTrue(controller.getNumberOfConsumers() == 1);
        Assert.assertTrue(controller.getMinLatencyCrossConsumers() == Integer.MAX_VALUE);
        Assert.assertTrue(controller.getMaxLatencyCrossConsumers() == 0);
        Assert.assertTrue(controller.getAverageLatencyCrossConsumers() == 0);
        Assert.assertTrue(controller.getMessages().size() == 0);
        Assert.assertTrue(controller.getNumberOfRunningConsumers() == 1);
        controller.stopConsumers();
        Assert.assertTrue(controller.getNumberOfRunningConsumers() == 0);
    }

    @Test
    public void shouldHaveThreeConsumerReady(){
        KafkaConfiguration cfg = new KafkaConfiguration();
         // 20 msgs, 3 partitions on topic
        PerfConsumerController controller = new PerfConsumerController(20,3,cfg);
        Assert.assertNotNull(controller);
        Assert.assertTrue(controller.getNumberOfConsumers() == 3);
        Assert.assertTrue(controller.getNumberOfRunningConsumers() == 3);
        controller.stopConsumers();
        Assert.assertTrue(controller.getNumberOfRunningConsumers() == 0);
    }
}