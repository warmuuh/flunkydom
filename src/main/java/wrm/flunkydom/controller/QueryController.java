package wrm.flunkydom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.RedirectView;
import wrm.flunkydom.persistence.AgentConfig;
import wrm.flunkydom.persistence.AgentRepository;
import wrm.flunkydom.persistence.Goal;
import wrm.flunkydom.persistence.Goal.Artifact;
import wrm.flunkydom.persistence.GoalRepository;
import wrm.llm.agent.AgentScheduler;
import wrm.llm.agent.AgentScheduler.TaskLifecycleListener;
import wrm.llm.agent.AgentTask;

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
        goal, agentName, goal.artifacts().stream().map(QueryController::contentToDetail).toList()
    ));
  }

  @GetMapping("/artifact")
  @ResponseBody
  public String getGoalDetails(@RequestParam("id") String goalId, @RequestParam("file") String filename) {
    Goal goal = goalRepository.findById(goalId);
    return goal.artifacts().stream().filter(a -> a.filename().equals(filename))
        .findAny()
        .map(a -> contentToString(a))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @NotNull
  private static ArtifactDetail contentToDetail(Artifact a) {
    String extension = a.filename().substring(a.filename().lastIndexOf(".") + 1);
    String highlighter = switch (extension) {
      case "md" -> "markdown";
      case "py" -> "python";
      case "c" -> "c";
      case "cpp" -> "cpp";
      case "h" -> "cpp";
      case "css" -> "css";
      case "go" -> "go";
      case "java" -> "java";
      case "js" -> "javascript";
      case "json" -> "json";
      case "xml" -> "xml";
      case "php" -> "php";
      case "rb" -> "ruby";
      case "rs" -> "rust";
      case "sh" -> "bash";
      case "sql" -> "sql";
      case "ts" -> "typescript";
      case "yaml" -> "yaml";
      case "txt" -> "plaintext";
      default -> "java";
    };
    return new ArtifactDetail(
        a.filename(), contentToString(a), highlighter
    );
  }

  private static String contentToString(Artifact a) {
    if (a.content() == null || a.content().length == 0) {
      return "-- empty --";
    }
    return new String(a.content());
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
        agent,
        List.of()
    );
    goalRepository.addNewGoal(newGoal);
    scheduler.schedule(newGoal.inputQuery(), newGoal.id());
    return new RedirectView("/query/details?id=" + newGoal.id());
  }

  @PostMapping("/delete")
  public RedirectView deleteGoal(@RequestParam("id") String goalId) {
    goalRepository.deleteById(goalId);
    return new RedirectView("/query");
  }

  @GetMapping(value = "/listen", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter listenToGoal(@RequestParam("id") String goalId) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

    Goal goal = goalRepository.findById(goalId);
    sendSseUpdate(goal.log(), null, emitter, goalId, () -> {});

    if (!goal.status().equals("FINISHED")) {
      scheduler.addLifecycleListener(new TaskLifecycleListener() {
        {
          emitter.onCompletion(() -> scheduler.removeLifecycleListener(this));
          emitter.onTimeout(() -> scheduler.removeLifecycleListener(this));
        }

        @Override
        public void onCompleted(AgentTask task) {
          if (task.parentId().equals(goalId)) {
            sendSseUpdate(task.prompt(), null, emitter, goalId, () -> scheduler.removeLifecycleListener(this));
            sendSseState("FINISHED", task.result().get(), emitter, goalId, () -> scheduler.removeLifecycleListener(this));
            scheduler.removeLifecycleListener(this);
          }
        }

        @Override
        public void onError(AgentTask task, Exception e) {
          if (task.parentId().equals(goalId)) {
            sendSseUpdate(task.prompt(), e.toString(), emitter, goalId, () -> scheduler.removeLifecycleListener(this));
            sendSseState("ERROR", e.toString(), emitter, goalId, () -> scheduler.removeLifecycleListener(this));
            scheduler.removeLifecycleListener(this);
          }
        }

        @Override
        public void onProcess(AgentTask task) {
          if (task.parentId().equals(goalId)) {
            sendSseUpdate(task.prompt(), null, emitter, goalId, () -> scheduler.removeLifecycleListener(this));
          }
        }
      });
    }
    return emitter;
  }

  private static void sendSseState(String state, String result, SseEmitter emitter, String goalId,
      Runnable errorHandler) {
    try {
      ListenGoalStateEvent object = new ListenGoalStateEvent(state, result);
      ObjectMapper mapper = new ObjectMapper();
      emitter.send(SseEmitter.event()
          .name("state")
          .id(goalId)
          .data(mapper.writeValueAsString(object)));
    } catch (IOException e) {
      errorHandler.run();
    }
  }

  private static void sendSseUpdate(String prompt, String error, SseEmitter emitter, String goalId,
      Runnable errorHandler) {
    try {
      ListenGoalEvent object = new ListenGoalEvent(prompt, error);
      ObjectMapper mapper = new ObjectMapper();
      emitter.send(SseEmitter.event()
          .name("update")
          .id(goalId)
          .data(mapper.writeValueAsString(object)));
    } catch (IOException e) {
      errorHandler.run();
    }
  }

  public record ListenGoalStateEvent(String status, String result) {

  }

  public record ListenGoalEvent(String prompt, String error) {

  }


  public record GetGoalsModel(List<Goal> goals, List<AgentConfig> agents) {

  }

  public record GetGoalDetailModel(Goal goal, String agent, List<ArtifactDetail> artifacts) {

  }

  public record ArtifactDetail(String filename, String content, String highlighter) {

  }

}
