package wrm.llm.agent;

import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessor;
import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessorConfig;
import ai.knowly.langtoch.llm.schema.io.SingleText;
import ai.knowly.langtoch.prompt.annotation.Prompt;
import ai.knowly.langtoch.prompt.annotation.PromptProcessor;
import com.theokanning.openai.service.OpenAiService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import wrm.llm.tools.ChatGptFunction.OpenAiConfig;
import wrm.llm.tools.Tool.ToolOutcome;


public class AutoGptAgent extends Agent {

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
  public static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input: (.*)", Pattern.DOTALL);
  public static final Pattern FINAL_ANSWER_PATTERN = Pattern.compile("Final Answer: (.*)", Pattern.DOTALL);

  private final ToolExecutor tools;
  private final Supplier<OpenAiConfig> aiConfigSupplier;

  public AutoGptAgent(ToolExecutor tools, Supplier<OpenAiConfig> aiConfigSupplier) {
    super("autogpt");
    this.aiConfigSupplier = aiConfigSupplier;
    this.tools = tools;
  }

  @Override
  public AgentTask createInitialTask(String parentId, String query) {
    var currentPrompt = PromptProcessor.createPromptTemplate(AgentPrompt.class,
            new AgentPrompt(query, tools.getToolDescriptions(parentId)))
        .format();
    System.out.print(currentPrompt);
    AgentTask currentTask = new AgentTask(parentId, 0, currentPrompt, Optional.empty(), List.of(), Map.of());
    return currentTask;
  }


  @Override
  public NextTask iterateOnTask(AgentTask task) {
    OpenAITextProcessor aiTextProcessor = createTextProcessor();
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

    AgentTask newTask = new AgentTask(task.parentId(), task.step() + 1, newPrompt.toString(), result, List.of(), Map.of());
    return new NextTask(newTask, scheduleContinuation);
  }


  private OpenAITextProcessor createTextProcessor() {
    OpenAiConfig openAiConfig = aiConfigSupplier.get();
    var aiTextProcessor = OpenAITextProcessor.create(new OpenAiService(openAiConfig.token()));
    aiTextProcessor.withConfig(OpenAITextProcessorConfig.builder()
        .setModel("text-davinci-003")
        .setMaxTokens(2048)
        .setTemperature(0.7)
        .setMaxTokens(256)
        .setStream(false)
        .setStop(List.of("Observation:"))
        .build());
    return aiTextProcessor;
  }

  private ToolOutcome executeAction(String actionName, String actionInput) {
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

  @Prompt(
      template = """
        Answer the following questions as best you can. You have access to the following tools:
                
        {{$toolDescriptions}}
                
        Use the following format:
                
        Question: the input question you must answer
        Thought: you should always think about what to do
        Action: the action to take, should be one of [{{$tools}}]
        Action Input: the input to the action
        Observation: the result of the action
        ... (this Thought/Action/Action Input/Observation can repeat N times)
        Thought: I now know the final answer
        Final Answer: the final answer to the original input question
                
        Begin!
                
        Question: {{$question}}
        Thought: """,
      variables = {"question", "toolDescriptions", "tools"}
  )
  public class AgentPrompt {
    private final String question;
    private final String toolDescriptions;
    private final String tools;

    public AgentPrompt(String question, Map<String, String> toolDescriptions) {
      this.question = question;
      this.toolDescriptions = toolDescriptions.entrySet().stream().map(t -> t.getKey() + ": " + t.getValue()).collect(
          Collectors.joining("\n"));
      this.tools = String.join(", ", toolDescriptions.keySet());
    }
  }
}
