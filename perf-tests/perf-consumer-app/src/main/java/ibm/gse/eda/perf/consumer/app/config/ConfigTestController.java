package ibm.gse.eda.perf.consumer.app.config;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import ibm.gse.eda.perf.consumer.kafka.KafkaConfiguration;

@Path("/config")
@RequestScoped
public class ConfigTestController {

    @Inject
    private KafkaConfiguration config;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getInjectedConfigValue() {
        return config.getPropertiesAsString();
    }


}
