package wrm.llm.agent;

import java.util.Map;
import java.util.Optional;

public record AgentTask (
    String parentId,
    int step,
    String prompt,
    Optional<String> result,
    Map<String, String> customData
){

  public boolean isCompleted() {
    return  result.isPresent();
  }
}
