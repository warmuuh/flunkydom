package wrm.llm.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import wrm.llm.tools.ZapierFunction.ZapierConfiguration;

public class ZapierFunction extends Tool<ZapierConfiguration> {

  private final String actionId;
  private final OkHttpClient client;


  public ZapierFunction(String actionId, String id, String description) {
    super(id, description);
    this.actionId = actionId;
    client = new OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(60))
        .build();
  }


  @Override
  public String getToolConfigId() {
    //this tool as a dynamic id, based on whats registered at zapier. for config loading, we have to use a static id
    return "zapier";
  }

  @Override
  public ToolOutcome execute(String input, ZapierConfiguration config) {
    try {
      String response = client.newCall(new Builder()
          .url("https://nla.zapier.com/api/v1/dynamic/exposed/%s/execute/".formatted(actionId))
          .header("x-api-key", config.token())
          .post(RequestBody.create(MediaType.get("application/json"), """
              {
                "instructions": "%s"
              }""".formatted(input)))
          .build()).execute().body().string();
      return ToolOutcome.of(response);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static Map<String, String> getActionIds(String zapierToken) {
    Map<String, String> resultMap = new HashMap<>();
    try {
      String response = new OkHttpClient().newCall(new Builder()
          .url("https://nla.zapier.com/api/v1/dynamic/exposed/")
          .header("x-api-key", zapierToken)
          .build()).execute().body().string();

      JsonNode jsonNode = new ObjectMapper().readTree(response);
      for (JsonNode result : jsonNode.get("results")) {
        resultMap.put(result.get("id").textValue(), result.get("description").textValue());
      }
      return resultMap;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public record ZapierConfiguration(String token) {

  }
}
