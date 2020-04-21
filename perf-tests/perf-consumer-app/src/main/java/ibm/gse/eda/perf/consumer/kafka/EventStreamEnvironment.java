package ibm.gse.eda.perf.consumer.kafka;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EventStreamEnvironment {
    private static final Logger logger = Logger.getLogger(EventStreamEnvironment.class.getName());
  
    public static EventStreamsCredentials getEventStreamsCredentials() {
        String vcapServices = System.getenv("VCAP_SERVICES");
        logger.log(Level.INFO, "VCAP_SERVICES: \n" + vcapServices);
        if (vcapServices == null) {
            logger.log(Level.SEVERE, "VCAP_SERVICES environment variable is null.");
            throw new IllegalStateException("VCAP_SERVICES environment variable is null.");
        }
        return transformVcapServices(vcapServices);
    }

    private static EventStreamsCredentials transformVcapServices(String vcapServices) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode instanceCredentials = mapper.readValue(vcapServices, JsonNode.class);
            // when running in CloudFoundry VCAP_SERVICES is wrapped into a bigger JSON object
            // so it needs to be extracted. We attempt to read the "instance_id" field to identify
            // if it has been wrapped
            if (instanceCredentials.get("instance_id") == null) {
                Iterator<String> it = instanceCredentials.fieldNames();
                // Find the Event Streams service bound to this application.
                while (it.hasNext()) {
                    String potentialKey = it.next();
                    String messageHubJsonKey = "messagehub";
                    if (potentialKey.startsWith(messageHubJsonKey)) {
                        logger.log(Level.WARNING, "Using the '" + potentialKey + "' key from VCAP_SERVICES.");
                        instanceCredentials = instanceCredentials.get(potentialKey)
                                .get(0)
                                .get("credentials");
                        break;
                    }
                }
            }
            return mapper.readValue(instanceCredentials.toString(), EventStreamsCredentials.class);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "VCAP_SERVICES environment variable parses failed.");
            throw new IllegalStateException("VCAP_SERVICES environment variable parses failed.", e);
        }
    }
}