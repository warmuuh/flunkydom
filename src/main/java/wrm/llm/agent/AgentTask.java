package wrm.llm.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import wrm.flunkydom.persistence.Goal.Artifact;

public record AgentTask (
    String parentId,
    int step,
    String prompt,
    Optional<String> result,
    List<Artifact> artifacts,
    Map<String, String> customData
){

  public boolean isCompleted() {
    return  result.isPresent();
  }
}
