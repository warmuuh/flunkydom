package wrm.flunkydom;

import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessor;
import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessorConfig;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import wrm.flunkydom.persistence.EmbeddingRepository;
import wrm.flunkydom.service.EmbeddingService;
import wrm.flunkydom.service.ToolConfigService;
import wrm.flunkydom.service.ToolService;
import wrm.llm.agent.Agent;
import wrm.llm.tools.ChatGptFunction.OpenAiConfig;

@Configuration
@DependsOn("toolsConfiguration")
public class AgentConfiguration {

  @Bean
  EmbeddingService embeddingService(ToolConfigService toolConfigService, EmbeddingRepository embeddingRepository) {
    //TODO: move openai service creation to runtime
    OpenAiConfig openai = toolConfigService.loadToolConfig("openai", OpenAiConfig.class);
    OpenAiService aiService = new OpenAiService(openai.token());
    return new EmbeddingService(aiService, embeddingRepository);
  }

  @Bean
  Agent agent(ToolConfigService toolConfigService, ToolService toolService) {
    //TODO: move openai service creation to runtime
    OpenAiConfig openai = toolConfigService.loadToolConfig("openai", OpenAiConfig.class);
    var aiTextProcessor = OpenAITextProcessor.create(new OpenAiService(openai.token()));
    aiTextProcessor.withConfig(OpenAITextProcessorConfig.builder()
        .setModel("text-davinci-003")
        .setMaxTokens(2048)
        .setTemperature(0.7)
        .setMaxTokens(256)
        .setStream(false)
        .setStop(List.of("Observation:"))
        .build());

    return new Agent(aiTextProcessor, toolService);
  }

}
