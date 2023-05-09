package wrm.llm.tools;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import ai.knowly.langtoch.tool.Function;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import wrm.llm.tools.GoogleSerpFunction.SerpapiConfig;


public class GoogleSerpFunction extends Tool<SerpapiConfig> {

  private final OkHttpClient client = new OkHttpClient();

  public GoogleSerpFunction() {
    super("ask", """
        useful for when you need to answer questions about current events or other simple information.\s
        can mainly respond to simple questions.\s
        input should be a search query or a question.""");
  }

  @Override
  public String getToolConfigId() {
    return "serpapi";
  }

  @Override
  public ToolOutcome execute(String input, SerpapiConfig configuration) {
    try {
      String response = client.newCall(new Builder()
          .url("https://serpapi.com/search?api_key=%s&q=%s".formatted(configuration.token(), input))
          .build()).execute().body().string();
      JsonNode jsonNode = new ObjectMapper().readTree(response);
      String result = ofNullable(jsonNode.get("answer_box"))
          .map(box -> ofNullable(box.get("answer")).orElse(box.get("snippet")))
          .map(JsonNode::toString)
          .orElse("nothing found");
      return new ToolOutcome(result, empty());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public record SerpapiConfig(String token) {

  }
}
