package it;

import static org.junit.jupiter.api.Assertions.assertEquals;
    
import java.util.HashMap;

import javax.json.JsonArray;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HealthITests {
 
      private JsonArray servicesStates;
      private static HashMap<String, String> endpointData;
    
      private String HEALTH_ENDPOINT = "health";
      private String READINESS_ENDPOINT = "health/ready";
      private String LIVENES_ENDPOINT = "health/live";
    
      @BeforeEach
      public void setup() {
        endpointData = new HashMap<String, String>();
      }
    
      @Test
      public void testIfServicesAreUp() {
        endpointData.put("SystemResourceReadiness", "UP");
        endpointData.put("SystemResourceLiveness", "UP");
        endpointData.put("InventoryResourceReadiness", "UP");
        endpointData.put("InventoryResourceLiveness", "UP");
    
        servicesStates = HealthITUtil.connectToHealthEnpoint(200, HEALTH_ENDPOINT);
        checkStates(endpointData, servicesStates);
      }
    
      @Test
      public void testReadiness() {
        endpointData.put("SystemResourceReadiness", "UP");
        endpointData.put("InventoryResourceReadiness", "UP");
    
        servicesStates = HealthITUtil.connectToHealthEnpoint(200, READINESS_ENDPOINT);
        checkStates(endpointData, servicesStates);
      }
    
      @Test
      public void testLiveness() {
        endpointData.put("SystemResourceLiveness", "UP");
        endpointData.put("InventoryResourceLiveness", "UP");
    
        servicesStates = HealthITUtil.connectToHealthEnpoint(200, LIVENES_ENDPOINT);
        checkStates(endpointData, servicesStates);
      }
    
    
      private void checkStates(HashMap<String, String> testData, JsonArray servStates) {
        testData.forEach((service, expectedState) -> {
          assertEquals(expectedState, HealthITUtil.getActualState(service, servStates),
              "The state of " + service + " service is not matching.");
        });
      }
    
}