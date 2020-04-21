package it;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;

public class HealthITUtil {
    private static String port;
    private static String baseUrl;
  
    static {
      port = System.getProperty("default.http.port");
      baseUrl = "http://localhost:" + port + "/";
    }
  
    public static JsonArray connectToHealthEnpoint(int expectedResponseCode,
        String endpoint) {
      String healthURL = baseUrl + endpoint;
      Client client = ClientBuilder.newClient().register(JsrJsonpProvider.class);
      Response response = client.target(healthURL).request().get();
      assertEquals(expectedResponseCode, response.getStatus(),
          "Response code is not matching " + healthURL);
      JsonArray servicesStates = response.readEntity(JsonObject.class)
          .getJsonArray("checks");
      response.close();
      client.close();
      return servicesStates;
    }
  
    public static String getActualState(String service, JsonArray servicesStates) {
      String state = "";
      for (Object obj : servicesStates) {
        if (obj instanceof JsonObject) {
          if (service.equals(((JsonObject) obj).getString("name"))) {
            state = ((JsonObject) obj).getString("status");
          }
        }
      }
      return state;
    }
  
}