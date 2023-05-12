package wrm.flunkydom.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import wrm.flunkydom.persistence.AgentConfig;
import wrm.flunkydom.persistence.AgentRepository;
import wrm.flunkydom.persistence.Goal;
import wrm.flunkydom.persistence.GoalRepository;
import wrm.llm.agent.AutoGptAgent.ToolExecutor;
import wrm.llm.tools.Tool;
import wrm.llm.tools.Tool.ToolOutcome;

@Component
public class ToolService implements ToolExecutor {
  private final ToolConfigService toolConfigService;
  private final List<Tool<?>> tools;
  private final GoalRepository goalRepository;
  private final AgentRepository agentRepository;

  public ToolService(ToolConfigService toolConfigService, List<Tool<?>> tools, GoalRepository goalRepository,
      AgentRepository agentRepository) {
    this.toolConfigService = toolConfigService;
    this.tools = tools;
    this.goalRepository = goalRepository;
    this.agentRepository = agentRepository;
  }

  public List<Tool<?>> getAllTools() {
    return tools;
  }

  @Override
  public Map<String, String> getToolDescriptions(String parentId) {
    Goal goal = goalRepository.findById(parentId);
    AgentConfig agentConfig = agentRepository.findById(goal.agent());

    return tools.stream()
      .filter(t -> agentConfig.activeTools().contains(t.getClass().getSimpleName()))
      .collect(Collectors.toMap(
        t -> t.getId(),
        t -> t.getDescription()
    ));
  }

  @Override
  public ToolOutcome executeTool(String toolId, String input) {
    if (toolId.equalsIgnoreCase("none")) {
      return ToolOutcome.of("none");
    }

    Tool tool = tools.stream().filter(t -> t.getId().equals(toolId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No tool found for id: " + toolId));

    Object configObj = toolConfigService.loadToolConfig(tool.getToolConfigId(), tool.getConfigClass());
    return tool.execute(input, configObj);
  }
}

