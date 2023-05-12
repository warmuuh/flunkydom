package wrm.flunkydom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import wrm.flunkydom.persistence.ToolConfiguration;
import wrm.flunkydom.persistence.ToolRepository;

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

