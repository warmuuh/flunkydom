package wrm.flunkydom.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import wrm.flunkydom.persistence.AgentConfig;
import wrm.flunkydom.persistence.AgentRepository;
import wrm.flunkydom.persistence.Goal;
import wrm.flunkydom.persistence.GoalRepository;
import wrm.llm.agent.AgentScheduler;

@Controller
@RequestMapping("/query")
public class QueryController {

  private final GoalRepository goalRepository;
  private final AgentRepository agentRepository;
  private final AgentScheduler scheduler;

  public QueryController(GoalRepository goalRepository, AgentRepository agentRepository, AgentScheduler scheduler) {
    this.goalRepository = goalRepository;
    this.agentRepository = agentRepository;
    this.scheduler = scheduler;
  }

  @GetMapping
  public ModelAndView getGoals() {
    List<Goal> goals = goalRepository.findAll().stream().sorted(Comparator.comparing(Goal::creationTime)).toList();
    List<AgentConfig> agents = agentRepository.findAll();
    return new ModelAndView("goals", "model", new GetGoalsModel(goals, agents));
  }

  @GetMapping("/details")
  public ModelAndView getGoalDetails(@RequestParam("id") String goalId) {
    Goal goal = goalRepository.findById(goalId);
    String agentName = "NOT FOUND";
    if (goal.agent() != null) {
      AgentConfig agent = agentRepository.findById(goal.agent());
      agentName = agent.name();
    }
    return new ModelAndView("goal-detail", "model", new GetGoalDetailModel(
        goal, agentName
    ));
  }

  @PostMapping("/new")
  public RedirectView addGoal(@RequestParam("query") String query, @RequestParam("agent") String agent) {
    Goal newGoal = new Goal(
        UUID.randomUUID().toString(),
        query,
        Instant.now(),
        null,
        "CREATED",
        null,
        null,
        0,
        agent
    );
    goalRepository.addNewGoal(newGoal);
    scheduler.schedule(newGoal.inputQuery(), newGoal.id());
    return new RedirectView("/query");
  }

  @PostMapping("/delete")
  public RedirectView deleteGoal(@RequestParam("id") String goalId) {
    goalRepository.deleteById(goalId);
    return new RedirectView("/query");
  }

  public record GetGoalsModel(List<Goal> goals, List<AgentConfig> agents) {

  }

  public record GetGoalDetailModel(Goal goal, String agent) {

  }
}
