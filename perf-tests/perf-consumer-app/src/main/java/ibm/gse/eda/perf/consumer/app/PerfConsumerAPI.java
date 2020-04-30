package ibm.gse.eda.perf.consumer.app;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import ibm.gse.eda.perf.consumer.app.dto.Control;
import ibm.gse.eda.perf.consumer.kafka.PerfConsumerController;

@Path("/perf")
@ApplicationScoped
public class PerfConsumerAPI {
    
    @Inject
    protected PerfConsumerController perfConsumerController;

    @Inject 
    @ConfigProperty(name = "app.version", defaultValue = "0.0.1")
    protected String appVersion;

    public void init(@Observes 
    @Initialized(ApplicationScoped.class) Object context) {
        System.out.println("Performance Consumer App started. Version:" + appVersion);
        System.out.println(perfConsumerController.toString()); // triggers instance creation from CDI instead of using proxy
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve consumer controller configuration",description="Retrieve consumer controller configuration")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Consumer controller configuration",
                content = @Content(mediaType = "application/json")
            ) 
     })
     public String getConsumerConfig(){ 
        System.out.println("getConsumerConfig " + perfConsumerController.toString());
        StringBuffer sb = new StringBuffer();
        sb.append("{ \"version\": \"");
        sb.append(appVersion);
        sb.append("\",\"MaxPoolSize\":");
        sb.append(perfConsumerController.getMaxConsumers());
        sb.append(",\"CurrentPoolSize\":");
        sb.append(perfConsumerController.getNumberOfConsumers());
        sb.append(",\"RunningConsumers\":");
        sb.append(perfConsumerController.getNumberOfRunningConsumers());
        sb.append("}");
        return sb.toString();
     }


    @GET
    @Path("messages")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve last 50 messages",description=" Retrieve the last 50 messages consumed in the kafka consumer")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Last x messages",
                content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = String[].class))) 
     })
     public List<String> getMessages(){
        return perfConsumerController.getMessages();
     }

     @Gauge(name = "averageLatency",unit = MetricUnits.MICROSECONDS, description = "Average latency cross partition")
     public long getAverageLatency(){
        return perfConsumerController.getAverageLatencyCrossConsumers();
     }

     @Gauge(name = "maxLatency",unit = MetricUnits.MICROSECONDS, description = "Maximum latency cross partition")
     public long getMaxLatency(){
         return perfConsumerController.getMaxLatencyCrossConsumers();
     }

     @Gauge(name = "minLatency",unit = MetricUnits.MICROSECONDS, description = "Minimum latency cross partition")
     public long getMinLatency(){
         return perfConsumerController.getMinLatencyCrossConsumers();
     }

     @Gauge(name = "recordCount", absolute = true, unit = MetricUnits.NONE, description = "Number of records processed")
     public long getCount(){
         return perfConsumerController.getCountCrossConsumers();
     }

     @GET
     @Path("current")
     @Produces(MediaType.APPLICATION_JSON)
     @Operation(summary = "Retrieve current message latency metrics",description="Retrieve current message latency between produced message timestamp and consumed record timestamp")
     @APIResponses(
         value = {
             @APIResponse(
                 responseCode = "200",
                 description = "Last latency metrics",
                 content = @Content(mediaType = "application/json")) 
      })
     public String getCurrentMetrics(){
        StringBuffer sb = new StringBuffer();
        sb.append("{ \"Max-latency\":");
        sb.append(getMaxLatency());
        sb.append(",\"Min-latency\":");
        sb.append(getMinLatency());
        sb.append(", \"Average-latency\":");
        sb.append(getAverageLatency());
        sb.append("}");
        return sb.toString();
     }

    /**
     * Change the configuration of the consumers.
     * @param control
     */
     @PUT
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.TEXT_PLAIN)
     @Operation(summary = "Stop or restart consumer", description = "")
     @APIResponses(value = {
             @APIResponse(responseCode = "400", description = "Unknown order should be STOP, RESTART, UPDATE", content = @Content(mediaType = "text/plain")),
             @APIResponse(responseCode = "200", description = "Processed", content = @Content(mediaType = "text/plain")) })
     public void controlConsumers( Control control){
         if (perfConsumerController.controlConsumers(control)) {
            Response.status(Status.OK).build();
         } else {
            Response.status(Status.BAD_REQUEST).build();
         }
     }
}