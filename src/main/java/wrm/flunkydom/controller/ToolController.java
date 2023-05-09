package wrm.flunkydom.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import wrm.flunkydom.persistence.Goal;
import wrm.flunkydom.persistence.ToolConfiguration;
import wrm.flunkydom.persistence.ToolRepository;

@Controller
@RequestMapping("/tools")
public class ToolController {

  private final ToolRepository toolRepository;

  public ToolController(ToolRepository toolRepository) {
    this.toolRepository = toolRepository;
  }

  @GetMapping
  public ModelAndView getToolConfigurations() {
    Map<String, ToolConfigDao> tools = toolRepository.findAll().stream().collect(Collectors.toMap(
        tool -> tool.toolId(),
        tool -> new ToolConfigDao(tool.configJson())
    ));
    return new ModelAndView("tools", "model", new GetToolsModel(tools));
  }

  @PostMapping("/update")
  public RedirectView updateToolConfigurations(ToolUpdateRequest tools) {
    tools.getTools().forEach((tool, configDao) -> {
      toolRepository.findById(tool); //validate that this tool exists
      toolRepository.updateToolConfiguration(new ToolConfiguration(
          tool,
          configDao.getConfigJson()
      ));
    });
    return new RedirectView("/tools");
  }


  public record GetToolsModel(Map<String, ToolConfigDao> tools){}
  public static class ToolUpdateRequest{

    public Map<String, ToolConfigDao> getTools() {
      return tools;
    }

    public void setTools(Map<String, ToolConfigDao> tools) {
      this.tools = tools;
    }

    Map<String, ToolConfigDao> tools = new HashMap<>();
  }

  public static class ToolConfigDao {

    public String getConfigJson() {
      return configJson;
    }

    public void setConfigJson(String configJson) {
      this.configJson = configJson;
    }

    public ToolConfigDao() {
    }

    public ToolConfigDao(String configJson) {
      this.configJson = configJson;
    }

    private String configJson;
  }
}
