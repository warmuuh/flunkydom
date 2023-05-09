package wrm.flunkydom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import wrm.flunkydom.persistence.ToolConfiguration;
import wrm.flunkydom.persistence.ToolRepository;
import wrm.llm.agent.Agent.ToolExecutor;
import wrm.llm.tools.Tool;

@Component
public class ToolConfigService {

  private final ToolRepository toolRepository;
  private final ObjectMapper mapper = new ObjectMapper();


  public ToolConfigService(ToolRepository toolRepository) {
    this.toolRepository = toolRepository;
  }


  public <T> T loadToolConfig(String id, Class<T> configClass) {
    ToolConfiguration toolCfg = toolRepository.findById(id);
    try {
      return mapper.readValue(toolCfg.configJson(), configClass);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}

