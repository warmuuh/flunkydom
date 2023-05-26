package wrm.flunkydom;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

@Configuration
public class WebConfiguration {

  private final ObjectMapper objectMapper;
  private final ContentNegotiatingViewResolver resolver;

  public WebConfiguration(ObjectMapper objectMapper, ContentNegotiatingViewResolver resolver) {
    this.objectMapper = objectMapper;
    this.resolver = resolver;
  }

  @PostConstruct
  public void jsonViewResolver() {
    MappingJackson2JsonView jsonView = new MappingJackson2JsonView(objectMapper);
    jsonView.setExtractValueFromSingleKeyModel(true);
    jsonView.setPrefixJson(true);
    resolver.setDefaultViews(List.of(jsonView));
  }

}
