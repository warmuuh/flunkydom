package wrm.flunkydom;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import wrm.flunkydom.persistence.EmbeddingRepository;
import wrm.flunkydom.service.EmbeddingService;
import wrm.flunkydom.service.ToolConfigService;
import wrm.flunkydom.service.ToolService;
import wrm.llm.agent.Agent;
import wrm.llm.agent.AutoGptAgent;
import wrm.llm.agent.SmolDevAgent;
import wrm.llm.tools.ChatGptFunction.OpenAiConfig;

@Configuration
@DependsOn("toolsConfiguration")
public class AgentConfiguration {

  @Bean
  EmbeddingService embeddingService(ToolConfigService toolConfigService, EmbeddingRepository embeddingRepository) {
    return new EmbeddingService(embeddingRepository,
        () -> toolConfigService.loadToolConfig("openai", OpenAiConfig.class));
  }

  @Bean
  Agent autoGptAgent(ToolConfigService toolConfigService, ToolService toolService) {
    return new AutoGptAgent(toolService,
        () -> toolConfigService.loadToolConfig("openai", OpenAiConfig.class));
  }

  @Bean
  Agent smoldevAgent(ToolConfigService toolConfigService, ToolService toolService) {
    return new SmolDevAgent(() -> toolConfigService.loadToolConfig("openai", OpenAiConfig.class));
  }

}
