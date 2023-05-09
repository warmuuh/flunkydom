package wrm.llm.tools;

import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.Optional;

public abstract class Tool<T> {

  private final String id;

  private final String description;


  protected Tool(String id, String description) {
    this.id = id;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public String getToolConfigId() {
    return getId();
  }

  public String getDescription() {
    return description;
  }


  public abstract ToolOutcome execute(String input, T configuration);


  public Class<T> getConfigClass() {
    return (Class<T>) ((ParameterizedType) getClass()
        .getGenericSuperclass()).getActualTypeArguments()[0];
  }

  public record ToolOutcome(
      String result,
      Optional<Instant> scheduleContinuation
  ) {

    public static ToolOutcome of(String text) {
      return new ToolOutcome(text, Optional.empty());
    }
  }

}
