package wrm.flunkydom;

import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wrm.flunkydom.persistence.AgentConfig;
import wrm.flunkydom.persistence.AgentRepository;
import wrm.flunkydom.persistence.Goal;
import wrm.flunkydom.persistence.GoalRepository;
import wrm.llm.agent.Agent;
import wrm.llm.agent.AutoGptAgent;
import wrm.llm.agent.AgentScheduler;
import wrm.llm.agent.AgentScheduler.TaskLifecycleListener;

@Configuration
public class SchedulerConfiguration {

  @Bean
  AgentScheduler agentScheduler(DataSource datasource,
      GoalRepository goals,
      AgentRepository agentConfigs,
      ObjectProvider<Agent> agents,
      TaskLifecycleListener lifecycleListener) {
    AgentScheduler agentScheduler = AgentScheduler.create(
        datasource,
        parentId -> {
          Goal goal = goals.findById(parentId);
          AgentConfig agentConfig = agentConfigs.findById(goal.agent());
          return agents.stream()
              .filter(agent -> agent.getAgentId().equals(agentConfig.agentTemplateId()))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("No agent with id " + agentConfig.agentTemplateId() + " found"));
        });
    agentScheduler.setLifecycleListener(lifecycleListener);
    return agentScheduler;
  }

}
