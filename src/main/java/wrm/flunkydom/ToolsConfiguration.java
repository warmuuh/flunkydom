package wrm.flunkydom;

import java.util.List;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import wrm.flunkydom.service.EmbeddingService;
import wrm.flunkydom.service.ToolConfigService;
import wrm.llm.tools.ChatGptFunction;
import wrm.llm.tools.EmbeddingFunction;
import wrm.llm.tools.GoogleSerpFunction;
import wrm.llm.tools.HomeassistantFunction;
import wrm.llm.tools.Tool;
import wrm.llm.tools.WaitFunction;
import wrm.llm.tools.WeatherApiFunction;
import wrm.llm.tools.ZapierFunction;
import wrm.llm.tools.ZapierFunction.ZapierConfiguration;

@Configuration
public class ToolsConfiguration implements ApplicationContextAware {

  @Bean
  Tool<?> searchTool() {
    return new GoogleSerpFunction();
  }

  @Bean
  Tool<?> hassTool() {
    return new HomeassistantFunction();
  }

  @Bean
  Tool<?> weatherTool() {
    return new WeatherApiFunction();
  }

  @Bean
  Tool<?> waitTool() {
    return new WaitFunction();
  }

  @Bean
  Tool<?> embeddingTool(EmbeddingService embeddingService) {
    return new EmbeddingFunction(input -> {
          List<String> found = embeddingService.fetchSimilarContentFor(input);
          if (found.isEmpty()) {
            return null;
          }
          return found.get(0);
        });
  }

  @Bean
  Tool<?> assistantTool() {
    return new ChatGptFunction();
  }

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException {
    //TODO: move this to runtime
    ToolConfigService toolConfigService = ctx.getBean(ToolConfigService.class);
    ZapierConfiguration zapierConfig = toolConfigService.loadToolConfig("zapier", ZapierConfiguration.class);

    ZapierFunction.getActionIds(zapierConfig.token()).forEach((id, description) -> {
      String toolId = description.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase();
      ((ConfigurableApplicationContext) ctx).getBeanFactory()
          .registerSingleton("zapier-" + id.hashCode(), new ZapierFunction(id, toolId, description.replace(':', ' ')));
    });
  }
}
