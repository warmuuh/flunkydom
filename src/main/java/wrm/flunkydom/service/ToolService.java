package wrm.flunkydom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import wrm.flunkydom.persistence.ToolConfiguration;
import wrm.flunkydom.persistence.ToolRepository;
import wrm.llm.agent.Agent.ToolExecutor;
import wrm.llm.tools.Tool;
import wrm.llm.tools.Tool.ToolOutcome;

@Component
public class ToolService implements ToolExecutor {
  private final ToolConfigService toolConfigService;
  private final List<Tool<?>> tools;


  public ToolService(ToolConfigService toolConfigService, List<Tool<?>> tools) {
    this.toolConfigService = toolConfigService;
    this.tools = tools;
  }


  @Override
  public Map<String, String> getToolDescriptions() {
    return tools.stream().collect(Collectors.toMap(
        t -> t.getId(),
        t -> t.getDescription()
    ));
  }

  @Override
  public ToolOutcome executeTool(String toolId, String input) {
    Tool tool = tools.stream().filter(t -> t.getId().equals(toolId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No tool found for id: " + toolId));

    Object configObj = toolConfigService.loadToolConfig(tool.getToolConfigId(), tool.getConfigClass());
    return tool.execute(input, configObj);
  }
}

