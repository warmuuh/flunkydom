package wrm.llm.agent;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.kagkarlsson.scheduler.ScheduledExecutionsFilter;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Duration;
import java.time.Instant;
import javax.sql.DataSource;

public class AgentScheduler {


  private final DataSource dataSource;
  private final AgentRetriever agentRetriever;
  private Scheduler scheduler;
  private OneTimeTask<AgentTask> execTask;
  private TaskLifecycleListener lifecycleListener = new TaskLifecycleListener() {};

  public static AgentScheduler create(DataSource dataSource, AgentRetriever agentRetriever) {
    AgentScheduler scheduler = new AgentScheduler(dataSource, agentRetriever);
    scheduler.init();
    return scheduler;
  }

  private AgentScheduler(DataSource dataSource, AgentRetriever agentRetriever) {
    this.dataSource = dataSource;
    this.agentRetriever = agentRetriever;
  }

  public void init() {
    execTask = Tasks.oneTime("agent-tasks", AgentTask.class)
        .execute((task, ctx) -> executeTask(task));
    scheduler = Scheduler
        .create(dataSource, execTask)
        .serializer(new JacksonSerializer(m -> m.registerModule(new Jdk8Module())))
        .pollingInterval(Duration.ofSeconds(1))
        .registerShutdownHook()
        .build();

    scheduler.start();
  }

  public boolean hasTasks() {
    return !scheduler.getScheduledExecutionsForTask("agent-tasks", AgentTask.class,
            ScheduledExecutionsFilter.all().withPicked(false))
        .isEmpty();

  }

  public void schedule(String query, String parentId) {
    Agent agent = agentRetriever.getAgentForGoal(parentId);
    AgentTask task = agent.createInitialTask(parentId, query);
    scheduler.schedule(execTask.schedulableInstance(getTaskId(task), task));
  }

  private static String getTaskId(AgentTask task) {
    return "%s-%s".formatted(task.parentId(), task.step());
  }

  private void executeTask(TaskInstance<AgentTask> task) {
    AgentTask data = task.getData();
    try {
      Agent agent = agentRetriever.getAgentForGoal(data.parentId());
      NextTask nextTask = agent.iterateOnTask(data);
      AgentTask newAgentTask = nextTask.task();
      if (!newAgentTask.isCompleted()) {
        lifecycleListener.onProcess(newAgentTask);
        scheduler.schedule(
            new TaskInstance<>(execTask.getName(), getTaskId(newAgentTask), newAgentTask),
            nextTask.scheduleContinuation().orElse(Instant.now()));
      } else {
        lifecycleListener.onCompleted(newAgentTask);
      }
    } catch (Exception e) {
      lifecycleListener.onError(data, e);
    }
  }

  public void setLifecycleListener(TaskLifecycleListener lifecycleListener) {
    this.lifecycleListener = lifecycleListener;
  }

  public interface TaskLifecycleListener {
    default void onCompleted(AgentTask task) {}
    default void onError(AgentTask task, Exception e) {}
    default void onProcess(AgentTask task) {}
  }

  @FunctionalInterface
  public interface AgentRetriever {
    Agent getAgentForGoal(String goalId);
  }

}
