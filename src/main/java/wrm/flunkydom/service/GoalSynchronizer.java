package wrm.flunkydom.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import wrm.flunkydom.persistence.Goal;
import wrm.flunkydom.persistence.GoalRepository;
import wrm.llm.agent.AgentScheduler.TaskLifecycleListener;
import wrm.llm.agent.AgentTask;

@Component
public class GoalSynchronizer implements TaskLifecycleListener {

  private final Logger log = LoggerFactory.getLogger(GoalSynchronizer.class);
  private final GoalRepository repository;

  public GoalSynchronizer(GoalRepository repository) {
    this.repository = repository;
  }

  @Override
  public void onCompleted(AgentTask task) {
    Goal goal = repository.findById(task.parentId());
    repository.updateGoal(new Goal(
        goal.id(),
        goal.inputQuery(),
        goal.creationTime(),
        Instant.now(),
        "FINISHED",
        task.result().get(),
        task.prompt(),
        task.step()
    ));
  }

  @Override
  public void onError(AgentTask task, Exception e) {
    log.error("Failed on task", e);
    Goal goal = repository.findById(task.parentId());
    repository.updateGoal(new Goal(
        goal.id(),
        goal.inputQuery(),
        goal.creationTime(),
        Instant.now(),
        "ERROR",
        e.toString(),
        task.prompt(),
        task.step()
    ));
  }

  @Override
  public void onProcess(AgentTask task) {
    Goal goal = repository.findById(task.parentId());
    repository.updateGoal(new Goal(
        goal.id(),
        goal.inputQuery(),
        goal.creationTime(),
        goal.finishTime(),
        "PROCESSING",
        goal.result(),
        task.prompt(),
        task.step()
    ));
  }
}
