package ibm.gse.eda.perf.consumer.app;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ibm.gse.eda.perf.consumer.app.dto.Control;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import ibm.gse.eda.perf.consumer.kafka.ConsumerRunnable;

/**
 *
 */
@Path("/perf")
@ApplicationScoped
public class PerfConsumerController {

    @Inject
    private ConsumerRunnable consumerRunnable;
    private ExecutorService executor;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve last 50 messages",description=" Retrieve the last 50 messages consumed in the kafka consumer")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "Last 50 messages",
                content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Message[].class))) 
     })
     public Collection<Message> getMessages(){
         return consumerRunnable.getMessages();
     }


     @Gauge(name = "averageLatency",unit = MetricUnits.MICROSECONDS)
     public long getAverageLatency(){
         return consumerRunnable.getAverageLatency();
     }

     @Gauge(name = "maxLatency",unit = MetricUnits.MICROSECONDS)
     public long getMaxLatency(){
         return consumerRunnable.getMaxLatency();
     }

     @Gauge(name = "minLatency",unit = MetricUnits.MICROSECONDS)
     public long getMinLatency(){
         return consumerRunnable.getMinLatency();
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

     private void createThread(int count ){
        executor = Executors.newFixedThreadPool(count);
        executor.execute(consumerRunnable);
     }

     @PUT
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.TEXT_PLAIN)
     @Operation(summary = "Stop or restart consumer", description = "")
     @APIResponses(value = {
             @APIResponse(responseCode = "400", description = "Unknown order should be STOP or START", content = @Content(mediaType = "text/plain")),
             @APIResponse(responseCode = "200", description = "Processed", content = @Content(mediaType = "text/plain")) })
     public void stopConsumer(Control control){
         if ("STOP".equals(control.order)) {
            consumerRunnable.stop();
            createThread(control.numOfConsumer);
            Response.status(Status.OK).build();
         }

         if ("START".equals(control.order)) {
            if (! consumerRunnable.isRunning()) {
                consumerRunnable.reStart();
                Response.status(Status.OK).build();
            }
         }
         Response.status(Status.BAD_REQUEST).build();
       
     }
}
