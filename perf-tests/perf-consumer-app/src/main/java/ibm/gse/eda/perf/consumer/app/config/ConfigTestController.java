package ibm.gse.eda.perf.consumer.app.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/config")
@RequestScoped
public class ConfigTestController {

    @Inject
    @ConfigProperty(name = "kafka.topic.name")
    private String injectedValue;

    @Path("/injected")
    @GET
    public String getInjectedConfigValue() {
        String results = "{ \"kafka.topic.name\": \""+ injectedValue + "\"}";
        return results;
    }

    @Path("/lookup")
    @GET
    public String getLookupConfigValue() {
        Config config = ConfigProvider.getConfig();
        String value = config.getValue("value", String.class);
        return "Config value from ConfigProvider " + value;
    }
}
