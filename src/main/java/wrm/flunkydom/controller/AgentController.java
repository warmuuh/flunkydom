package wrm.flunkydom.controller;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import wrm.flunkydom.persistence.AgentConfig;
import wrm.flunkydom.persistence.AgentRepository;
import wrm.flunkydom.service.ToolService;
import wrm.llm.agent.Agent;

@Controller
@RequestMapping("/agents")
public class AgentController {

  private final AgentRepository agentRepository;
  private final ToolService toolService;
  private final List<String> agentTemplates;

  public AgentController(AgentRepository agentRepository, ToolService toolService, List<Agent> registeredAgents) {
    this.agentRepository = agentRepository;
    this.toolService = toolService;
    this.agentTemplates = registeredAgents.stream().map(Agent::getAgentId).toList();
  }

  @GetMapping
  public ModelAndView getAgentConfigurations() {
    List<AgentConfig> agents = agentRepository.findAll();
    return new ModelAndView("agents", "model", new GetAgentsModel(
        agents,
        agentTemplates,
        getToolNames()
    ));
  }

  @GetMapping("/details")
  public ModelAndView getAgentDetails(@RequestParam("id") String agentId) {
    return new ModelAndView("agent-detail", "model", new GetAgentsDetailModel(
        agentRepository.findById(agentId),
        agentTemplates,
        getToolNames()));
  }

  @NotNull
  private Set<String> getToolNames() {
    return toolService.getAllTools().stream().map(t -> t.getClass().getSimpleName()).collect(Collectors.toSet());
  }

  @PostMapping("/new")
  public RedirectView addAgent(@RequestParam("name") String agentName,
      @RequestParam("template") String agentTemplate,
      @RequestParam(value = "tools", required = false) List<String> enabledTools) {
    agentRepository.addNewAgentConfiguration(new AgentConfig(
        UUID.randomUUID().toString(),
        agentName,
        agentTemplate,
        enabledTools != null ? enabledTools : List.of()
    ));
    return new RedirectView("/agents");
  }

  @PostMapping("/update")
  public RedirectView addAgent(@RequestParam("id") String id,
      @RequestParam("name") String agentName,
      @RequestParam("template") String agentTemplate,
      @RequestParam(value = "tools", required = false) List<String> enabledTools) {
    agentRepository.updateAgentConfiguration(new AgentConfig(
        id,
        agentName,
        agentTemplate,
        enabledTools != null ? enabledTools : List.of()
    ));
    return new RedirectView("/agents");
  }

  @PostMapping("/delete")
  public RedirectView deleteAgent(@RequestParam("id") String agentId) {
    agentRepository.deleteById(agentId);
    return new RedirectView("/agents");
  }

  public record GetAgentsModel(
      List<AgentConfig> agents,
      List<String> agentTemplates,
      Collection<String> tools
  ) {

  }

  public record GetAgentsDetailModel(
      AgentConfig agent,
      List<String> agentTemplates,
      Collection<String> tools
  ) {

  }
}
