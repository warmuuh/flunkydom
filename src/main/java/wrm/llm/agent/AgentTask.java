package wrm.llm.agent;

import java.util.Optional;

public record AgentTask (
    String parentId,
    int step,
    String prompt,
    Optional<String> result
){

  public boolean isCompleted() {
    return  result.isPresent();
  }
}
