package wrm.llm.agent;

import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessor;
import ai.knowly.langtoch.llm.schema.io.SingleText;
import ai.knowly.langtoch.prompt.annotation.PromptProcessor;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wrm.llm.tools.Tool.ToolOutcome;


public class Agent {

  public static final String ANSI_RESET = "\u001B[0m";
  public static final String ANSI_BLACK = "\u001B[30m";
  public static final String ANSI_RED = "\u001B[31m";
  public static final String ANSI_GREEN = "\u001B[32m";
  public static final String ANSI_YELLOW = "\u001B[33m";
  public static final String ANSI_BLUE = "\u001B[34m";
  public static final String ANSI_PURPLE = "\u001B[35m";
  public static final String ANSI_CYAN = "\u001B[36m";
  public static final String ANSI_WHITE = "\u001B[37m";
  public static final Pattern ACTION_PATTERN = Pattern.compile("Action: (.*)");
  public static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input: (.*)");
  public static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("Final Answer: (.*)");

  private final OpenAITextProcessor aiTextProcessor;
  private final ToolExecutor tools;

  public Agent(OpenAITextProcessor aiTextProcessor, ToolExecutor tools) {
    this.aiTextProcessor = aiTextProcessor;
    this.tools = tools;
  }

  public String runAutonomously(String query) {
    AgentTask currentTask = createInitialTask("0", query);

    while(!currentTask.isCompleted()) {
      NextTask nextTask = iterateOnTask(currentTask);
      nextTask.scheduleContinuation().ifPresent(cont -> {
        try {
          Thread.sleep(Instant.now().until(cont, ChronoUnit.MILLIS));
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
      currentTask = nextTask.task();
    }
    return currentTask.result().get();
  }

  public AgentTask createInitialTask(String parentId, String query) {
    var currentPrompt = PromptProcessor.createPromptTemplate(AgentPrompt.class,
            new AgentPrompt(query, tools.getToolDescriptions(parentId)))
        .format();
    System.out.print(currentPrompt);
    AgentTask currentTask = new AgentTask(parentId, 0, currentPrompt, Optional.empty());
    return currentTask;
  }


  public NextTask iterateOnTask(AgentTask task) {
    var response = aiTextProcessor.run(SingleText.of(task.prompt())).getText();
    System.out.print(ANSI_GREEN + response + ANSI_RESET);

    StringBuilder newPrompt = new StringBuilder(task.prompt());
    newPrompt.append(response);

    Optional<Instant> scheduleContinuation = Optional.empty();
    Optional<String> result = Optional.empty();

    Matcher action = ACTION_PATTERN.matcher(response);
    if (action.find()) {
      Matcher actionInput = ACTION_INPUT_PATTERN.matcher(response);
      if (!actionInput.find()) {
        throw new IllegalStateException("No action input found");
      }
      String actionName = action.group(1).toLowerCase();
      ToolOutcome actionOutput = executeAction(actionName, actionInput.group(1));
      String observation = "Observation: %s\n".formatted(actionOutput.result());
      scheduleContinuation = actionOutput.scheduleContinuation();
      System.out.print(ANSI_RED + observation + ANSI_RESET);
      newPrompt.append(observation);
    } else {
      Matcher finalAnswer = FINAL_ANSWER_PATTERN.matcher(response);
      if (finalAnswer.find()) {
        result = Optional.of(finalAnswer.group(1));
      } else {
        throw new IllegalStateException("Unknown conversation state");
      }
    }

    AgentTask newTask = new AgentTask(task.parentId(), task.step() + 1, newPrompt.toString(), result);
    return new NextTask(newTask, scheduleContinuation);
  }



  private ToolOutcome executeAction(String actionName, String actionInput) {
//    var actionFunction = tools.stream().filter(t -> t.name().equals(actionName))
//        .map(t -> t.functionRegistry().get(actionName))
//        .findFirst()
//        .orElseThrow(() -> new IllegalArgumentException("No tool known: " + actionName));
//    if (actionInput.find()){
//      actionOutput = actionFunction.execute(actionInput.group(1));
//    }  else {
//      throw new IllegalStateException("No Action Input found");
//    }

    ToolOutcome actionOutput = tools.executeTool(actionName, actionInput);

    if (actionOutput == null) {
      return ToolOutcome.of("could not find any result for this query");
    }
    return actionOutput;
  }

  public interface ToolExecutor {
    Map<String, String> getToolDescriptions(String parentId);
    ToolOutcome executeTool(String toolId, String input);
  }
}
