package wrm.flunkydom.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import wrm.flunkydom.persistence.Goal;
import wrm.flunkydom.persistence.GoalRepository;
import wrm.llm.agent.AgentScheduler;

@Controller
@RequestMapping("/query")
public class QueryController {

  private final GoalRepository goalRepository;
  private final AgentScheduler scheduler;

  public QueryController(GoalRepository goalRepository, AgentScheduler scheduler) {
    this.goalRepository = goalRepository;
    this.scheduler = scheduler;
  }

  @GetMapping
  public ModelAndView getGoals() {
    List<Goal> goals = goalRepository.findAll().stream().sorted(Comparator.comparing(Goal::creationTime)).toList();
    return new ModelAndView("goals", "model", new GetGoalsModel(goals));
  }

  @GetMapping("/details")
  public ModelAndView getGoalDetails(@RequestParam("id") String goalId) {
    return new ModelAndView("goal-detail", "model", new GetGoalDetailModel(goalRepository.findById(goalId)));
  }

  @PostMapping("/new")
  public RedirectView addGoal(@RequestParam("query") String query) {
    Goal newGoal = new Goal(
        UUID.randomUUID().toString(),
        query,
        Instant.now(),
        null,
        "CREATED",
        null,
        null,
        0
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

  public record GetGoalsModel(List<Goal> goals){}
  public record GetGoalDetailModel(Goal goal){}
}
