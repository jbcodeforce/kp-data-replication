package ibm.gse.eda.perf.consumer.app.health;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import ibm.gse.eda.perf.consumer.kafka.ConsumerRunnable;
import ibm.gse.eda.perf.consumer.kafka.KafkaConfiguration;

@Readiness
@ApplicationScoped
public class ServiceReadyHealthCheck implements HealthCheck {
    private static final Logger logger = Logger.getLogger(ServiceReadyHealthCheck.class.getName());
    private boolean ready = false;

    public ServiceReadyHealthCheck(){}
    
    @Override
    public HealthCheckResponse call() {

        return HealthCheckResponse.named(ServiceReadyHealthCheck.class.getSimpleName()).withData("ready",isReady()).up().build();
    }

    public boolean isReady(){
        return ready;
    }

    @Inject
    private ConsumerRunnable consumerRunnable;
    private ExecutorService executor;
    
	public void init(@Observes 
    @Initialized(ApplicationScoped.class) ServletContext context) {
        logger.log(Level.INFO,"App listener, start kafka consumer");
        // Initialise Kafka Consumer
        consumerRunnable.reStart();
        ready=consumerRunnable.isRunning();
    }

	
	public void destroy( @Observes 
    @Destroyed(ApplicationScoped.class) ServletContext contextServlet) {
        logger.log(Level.INFO,"APP ctx destroyed");
        consumerRunnable.stop();
        executor.shutdownNow();
        try {
            executor.awaitTermination(KafkaConfiguration.TERMINATION_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            logger.warning("await Termination( interrupted " + ie.getLocalizedMessage());
        }
	}
}
