package wrm.llm.tools;

import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessor;
import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessorConfig;
import ai.knowly.langtoch.llm.schema.io.SingleText;
import ai.knowly.langtoch.prompt.annotation.Prompt;
import ai.knowly.langtoch.prompt.annotation.PromptProcessor;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import java.util.Optional;
import wrm.llm.tools.ChatGptFunction.OpenAiConfig;


public class ChatGptFunction extends Tool<OpenAiConfig> {

  public ChatGptFunction() {
    super("assistant", """
        an assistant that you can ask whatever you want and it answers the question from its broad knowledge base.\s
        It can also generate text and imitate styles.\s
        It doesnt have any knowledge of your conversation so you need to give it all the information necessary in the input.""");
  }

  @Override
  public String getToolConfigId() {
    return "openai";
  }

  @Override
  public ToolOutcome execute(String input, OpenAiConfig configuration) {
    var aiTextProcessor = OpenAITextProcessor.create(new OpenAiService(configuration.token()));
    aiTextProcessor.withConfig(OpenAITextProcessorConfig.builder()
        .setModel("text-davinci-003")
        .setMaxTokens(2048)
        .setTemperature(0.7)
        .setMaxTokens(256)
        .setStream(false)
        .setStop(List.of("T/(=ASDT=/(")) // openai fails if there is no stopsign, so i put garbage in here
        .build());

    var currentPrompt = PromptProcessor.createPromptTemplate(ChatPrompt.class,
        new ChatPrompt(input));
    String response = aiTextProcessor.run(SingleText.of(currentPrompt.format())).getText();
    return new ToolOutcome(response, Optional.empty());
  }


  @Prompt(
      template = """
          Assistant is a large language model trained by OpenAI.
                  
          Assistant is designed to be able to assist with a wide range of tasks, from answering simple questions to providing in-depth explanations and discussions on a wide range of topics. As a language model, Assistant is able to generate human-like text based on the input it receives, allowing it to engage in natural-sounding conversations and provide responses that are coherent and relevant to the topic at hand.
                    
          Assistant is constantly learning and improving, and its capabilities are constantly evolving. It is able to process and understand large amounts of text, and can use this knowledge to provide accurate and informative responses to a wide range of questions. Additionally, Assistant is able to generate its own text based on the input it receives, allowing it to engage in discussions and provide explanations and descriptions on a wide range of topics.
                    
          Overall, Assistant is a powerful tool that can help with a wide range of tasks and provide valuable insights and information on a wide range of topics. Whether you need help with a specific question or just want to have a conversation about a particular topic, Assistant is here to assist.

          Human: {{$question}}
          Assistant: """,
      variables = {"question"}
  )
  public static class ChatPrompt {

    private final String question;

    public ChatPrompt(String question) {
      this.question = question;
    }
  }


  public record OpenAiConfig(String token) {

  }
}
