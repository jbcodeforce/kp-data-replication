package ibm.gse.eda.perf.consumer.app.health;

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

import ibm.gse.eda.perf.consumer.kafka.PerfConsumerController;

@Readiness
@ApplicationScoped
public class ServiceReadyHealthCheck implements HealthCheck {
    private static final Logger logger = Logger.getLogger(ServiceReadyHealthCheck.class.getName());
    @Inject
    private PerfConsumerController controller;
    
    public ServiceReadyHealthCheck(){}
    
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named(ServiceReadyHealthCheck.class.getSimpleName()).withData("ready",isReady()).up().build();
    }

    public boolean isReady(){
        return controller.isFullyRunning();
    }


	
	public void destroy( @Observes 
    @Destroyed(ApplicationScoped.class) ServletContext contextServlet) {
        logger.log(Level.WARNING,"APP ctx destroyed");
        controller.stopConsumers();        
	}
}
