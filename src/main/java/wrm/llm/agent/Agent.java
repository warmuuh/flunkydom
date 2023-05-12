package wrm.llm.agent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public abstract class Agent {

  private final String agentId;

  protected Agent(String agentId) {
    this.agentId = agentId;
  }

  abstract AgentTask createInitialTask(String parentId, String query);

  abstract NextTask iterateOnTask(AgentTask task);


  public String runAutonomously(String query) {
    AgentTask currentTask = createInitialTask("0", query);

    while(!currentTask.isCompleted()) {
      NextTask nextTask = iterateOnTask(currentTask);
      nextTask.scheduleContinuation().ifPresent(cont -> {
        try {
          Thread.sleep(Instant.now().until(cont, ChronoUnit.MILLIS));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
      currentTask = nextTask.task();
    }
    return currentTask.result().get();
  }

  public String getAgentId() {
    return agentId;
  }
}
