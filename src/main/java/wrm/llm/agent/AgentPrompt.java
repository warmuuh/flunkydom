package wrm.llm.agent;

import ai.knowly.langtoch.prompt.annotation.Prompt;
import ai.knowly.langtoch.tool.Tool;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    this.toolDescriptions = toolDescriptions.entrySet().stream().map(t -> t.getKey() + ": " + t.getValue()).collect(Collectors.joining("\n"));
    this.tools = String.join(", ", toolDescriptions.keySet());
  }
}


