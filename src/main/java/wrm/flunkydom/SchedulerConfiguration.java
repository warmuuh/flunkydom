package wrm.flunkydom;

import com.pgvector.PGvector;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wrm.llm.agent.Agent;
import wrm.llm.agent.AgentScheduler;
import wrm.llm.agent.AgentScheduler.TaskLifecycleListener;

@Configuration
public class SchedulerConfiguration {

  @Bean
  AgentScheduler agentScheduler(DataSource datasource, Agent agent, TaskLifecycleListener lifecycleListener) {
    AgentScheduler agentScheduler = AgentScheduler.create(datasource, agent);
    agentScheduler.setLifecycleListener(lifecycleListener);
    return agentScheduler;
  }

}
