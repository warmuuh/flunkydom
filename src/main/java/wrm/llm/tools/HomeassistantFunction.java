package wrm.llm.tools;

import static java.util.Optional.ofNullable;

import ai.knowly.langtoch.tool.Function;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import wrm.llm.tools.HomeassistantFunction.HomeassistConfig;


public class HomeassistantFunction extends Tool<HomeassistConfig> {

  private final OkHttpClient client = new OkHttpClient();
  Map<String, String> roomTranslations = Map.of(
      "office", "scene.arbeiten",
      "living room", "scene.entertainment",
      "kitchen", "scene.gutenmorgen",
      "bed room", "scene.schlafen"
  );

  public HomeassistantFunction() {
    super("hass", """
        an actor to prepare rooms for my arrival in my smart home.\s
        useful for when you need to prepare a room. input should be the name of the room""");
  }

  @Override
  public String getToolConfigId() {
    return "hass";
  }

  @Override
  public ToolOutcome execute(String input, HomeassistConfig configuration) {
    String sceneName = configuration.roomToSceneMapping().get(input.toLowerCase());
    if (sceneName == null) {
      return ToolOutcome.of("no scene found for room " + input);
    }

    try {
      Response response = client.newCall(new Builder()
          .url(configuration.endpoint() + "/api/services/scene/turn_on")
          .header("Authorization", "Bearer " + configuration.token())
          .header("Content-Type", "application/json")
          .post(RequestBody.create(MediaType.get("application/json"), """
              {
                "entity_id": "%s"
              }""".formatted(sceneName)))
          .build()).execute();
      if (!response.isSuccessful()) {
        throw new IllegalStateException("Failed to execute hass action: " + response.body().string());
      }
      return ToolOutcome.of(input + " activated.");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public record HomeassistConfig(String token, String endpoint, Map<String, String> roomToSceneMapping) {

  }
}
