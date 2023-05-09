package wrm.llm.tools;

import static java.util.Optional.ofNullable;

import ai.knowly.langtoch.tool.Function;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import wrm.llm.tools.WeatherApiFunction.WeatherapiConfig;


public class WeatherApiFunction extends Tool<WeatherapiConfig> {

  private final OkHttpClient client = new OkHttpClient();

  public WeatherApiFunction() {
    super("weather", """
        a lookup service for getting the current temperator at a given location. input should be the name of the location.""");
  }

  @Override
  public String getToolConfigId() {
    return "weatherapi";
  }

  @Override
  public ToolOutcome execute(String input, WeatherapiConfig config) {
    try {
      String response = client.newCall(new Builder()
          .url("https://api.weatherapi.com/v1/current.json?key=%s&q=%s&aqi=no".formatted(config.token(), input))
          .build()).execute().body().string();
      JsonNode jsonNode = new ObjectMapper().readTree(response);
      String location = ofNullable(jsonNode.get("location"))
          .map(box -> box.get("name").textValue() + " in " + box.get("country")).orElse("unknown");

      String temperature = ofNullable(jsonNode.get("current"))
          .map(box -> box.get("temp_c").decimalValue())
          .map(value -> value + " °C")
          .orElse("cannot find any temperature for " + input);
      return ToolOutcome.of("The temperature in " + location + " is " + temperature);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public record WeatherapiConfig(String token) {

  }
}
