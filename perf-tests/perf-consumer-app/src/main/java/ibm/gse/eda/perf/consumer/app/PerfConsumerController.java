package ibm.gse.eda.perf.consumer.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import ibm.gse.eda.perf.consumer.app.dto.Control;
import ibm.gse.eda.perf.consumer.kafka.CircularLinkedList;
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
    private ArrayList<ConsumerRunnable> threadList = new ArrayList<>();

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
     public CircularLinkedList<Message> getMessages(){
         return consumerRunnable.getMessages();
     }

     @Gauge(name = "averageLatency",unit = MetricUnits.MICROSECONDS)
     public long getAverageLatency(){
        long averageLatency = 0;
        for (ConsumerRunnable consumerRunnable : threadList) {
            averageLatency += consumerRunnable.getAverageLatency();
        }

        return averageLatency;
     }

     @Gauge(name = "maxLatency",unit = MetricUnits.MICROSECONDS)
     public long getMaxLatency(){
         long maxLatency = Long.MIN_VALUE;
         for (ConsumerRunnable consumerRunnable : threadList) {
            maxLatency = Long.max(maxLatency, consumerRunnable.getMaxLatency());
         }
         return maxLatency;
     }

     @Gauge(name = "minLatency",unit = MetricUnits.MICROSECONDS)
     public long getMinLatency(){
        long minLatency = Long.MAX_VALUE;
         for (ConsumerRunnable consumerRunnable : threadList) {
             minLatency = Long.min(minLatency, consumerRunnable.getMinLatency());
         }
         return minLatency;
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

//     private void createThread(int count ){
//     }

     @PUT
     @Consumes(MediaType.APPLICATION_JSON)
     @Produces(MediaType.TEXT_PLAIN)
     @Operation(summary = "Stop or restart consumer", description = "")
     @APIResponses(value = {
             @APIResponse(responseCode = "400", description = "Unknown order should be STOP or START", content = @Content(mediaType = "text/plain")),
             @APIResponse(responseCode = "200", description = "Processed", content = @Content(mediaType = "text/plain")) })
     public void stopConsumer(@Observes
     @Initialized(ApplicationScoped.class) Control control){
         if ("STOP".equals(control.order)) {
             for (ConsumerRunnable consumerRunnable : threadList) {
                 consumerRunnable.stop();
             }

             executor.shutdownNow();
             Response.status(Status.OK).build();
         }

         if ("START".equals(control.order)) {
            for (ConsumerRunnable consumerRunnable : threadList) {
                consumerRunnable.stop();
            }

            threadList = new ArrayList<>();
            executor = Executors.newFixedThreadPool(control.numOfConsumer);
            for (int i = 0; i< control.numOfConsumer; i++) {
                ConsumerRunnable runnable = new ConsumerRunnable(consumerRunnable.getConfig());
                executor.execute(runnable);
                threadList.add(runnable);
            }

            Response.status(Status.OK).build();

         }
         Response.status(Status.BAD_REQUEST).build();
       
     }
}