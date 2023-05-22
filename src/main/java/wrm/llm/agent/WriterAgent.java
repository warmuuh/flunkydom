package wrm.llm.agent;

import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessor;
import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessorConfig;
import ai.knowly.langtoch.llm.schema.io.SingleText;
import ai.knowly.langtoch.prompt.annotation.Prompt;
import ai.knowly.langtoch.prompt.annotation.PromptProcessor;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.util.Strings;
import wrm.flunkydom.persistence.Goal.Artifact;
import wrm.flunkydom.utils.CollectionUtils;
import wrm.llm.tools.ChatGptFunction.OpenAiConfig;

/**
 * based on https://github.com/smol-ai/developer
 */
public class WriterAgent extends Agent {

  private final Supplier<OpenAiConfig> aiConfigSupplier;

  public WriterAgent(Supplier<OpenAiConfig> aiConfigSupplier) {
    super("writer");
    this.aiConfigSupplier = aiConfigSupplier;
  }

  @Override
  public AgentTask createInitialTask(String parentId, String query) {
    var currentPrompt = PromptProcessor.createPromptTemplate(GenerateFigureDescription.class,
            new GenerateFigureDescription(query))
        .format();
    System.out.print(currentPrompt);
    AgentTask currentTask = new AgentTask(parentId, 0, currentPrompt, Optional.empty(), List.of(), Map.of(
        "state", "genfigures",
        "originalPrompt", query
    ));
    return currentTask;
  }


  @Override
  public NextTask iterateOnTask(AgentTask task) {
    OpenAITextProcessor aiTextProcessor = createTextProcessor();
    String state = task.customData().get("state");
    String query = task.customData().get("originalPrompt");

    if (state.equals("genfigures")) {
      var figures = aiTextProcessor.run(SingleText.of(task.prompt())).getText();

      var sharedDepPrompt = PromptProcessor.createPromptTemplate(GenerateSummary.class,
              new GenerateSummary(query, figures))
          .format();

      return new NextTask(new AgentTask(task.parentId(), 1, sharedDepPrompt, Optional.empty(),
          List.of(),
          Map.of(
              "state", "gensummary",
              "originalPrompt", query,
              "figures", figures
          )), Optional.empty());
    }  else if (state.equals("gensummary")) {
      var summary = aiTextProcessor.run(SingleText.of(task.prompt())).getText();

      var chaptersAndSummary = summary.split("\n\n");

      if (chaptersAndSummary.length > 1) {
        var generateFilePrompt = PromptProcessor.createPromptTemplate(GenerateChapter.class,
                new GenerateChapter(query,
                    chaptersAndSummary[0],
                    task.customData().get("figures"),
                    chaptersAndSummary[1]
                    ))
            .format();

        return new NextTask(new AgentTask(task.parentId(), 2000 + chaptersAndSummary.length,
            generateFilePrompt, Optional.empty(), List.of(), Map.of(
            "state", "genchapter",
            "originalPrompt", query,
            "figures", task.customData().get("figures"),
            "summary", chaptersAndSummary[0],
            "chapters", String.join("\n\n", Arrays.copyOfRange(chaptersAndSummary, 2, chaptersAndSummary.length)),
                "currentChapter", "1"
        )), Optional.empty());
      }
    } else if (state.equals("genchapter")) {
      String currentChapter = task.customData().get("currentChapter").trim();
      String figures = task.customData().get("figures");
      String summary = task.customData().get("summary");
      String[] chaptersToGenerate = task.customData().get("chapters").isEmpty()
          ? new String[]{}
          : task.customData().get("chapters").split("\n\n");

      List<Artifact> generatedArtifacts = new LinkedList<>();

      var fileContent = aiTextProcessor.run(SingleText.of(task.prompt())).getText();
      generatedArtifacts.add(new Artifact("chapter" + currentChapter + ".txt", fileContent.getBytes()));

      if (chaptersToGenerate.length > 0) {
        var generateFilePrompt = PromptProcessor.createPromptTemplate(GenerateChapter.class,
                new GenerateChapter(query,
                    summary,
                    figures,
                    chaptersToGenerate[0]
                ))
            .format();

        return new NextTask(new AgentTask(task.parentId(), 3000 + chaptersToGenerate.length + 1,
            generateFilePrompt, Optional.empty(), generatedArtifacts, Map.of(
            "state", "genchapter",
            "originalPrompt", query,
            "figures", figures,
            "summary", summary,
            "chapters", String.join("\n\n",  Arrays.copyOfRange(chaptersToGenerate, 1, chaptersToGenerate.length)),
                "currentChapter", ""+(Integer.parseInt(currentChapter) + 1)
        )), Optional.empty());
      } else {
        return new NextTask(new AgentTask(task.parentId(),
            2000,
            "",
            Optional.of(summary),
            generatedArtifacts,
            Map.of()),
            Optional.empty());
      }
    }

    throw new IllegalStateException("Unknown state: " + state);
  }


  private OpenAITextProcessor createTextProcessor() {
    OpenAiConfig openAiConfig = aiConfigSupplier.get();
    var aiTextProcessor = OpenAITextProcessor.create(new OpenAiService(openAiConfig.token(), Duration.ofSeconds(60)));
    aiTextProcessor.withConfig(OpenAITextProcessorConfig.builder()
        .setModel("text-davinci-003")
        .setMaxTokens(3000)
        .setTemperature(0.7)
        .setStream(false)
        .setStop(List.of("APSOJD:")) // empty list breaks
        .build());
    return aiTextProcessor;
  }


  @Prompt(
      template = """
          You are an AI writer who is trying to write a tale for the user based on their intent.
                    
          In response to the user's prompt:
                  
          ---
          the story is: {{$prompt}}
          ---
                   
          Think of some persons that could be part of the story. we need to understand what persons are part of the story and how they interact.
          Please name and briefly describe each person of the story and how their relation is to other persons.
          Exclusively focus on these descriptions, and do not add any other explanation.
          
          Example:
          
          Harry Jones: a detective with some drinking problem. Is intrigued by Sally.
          
          Sally: a blond femme fatal. First doesnt like Harry but learns to like him.
                   
          """,
      variables = {"prompt"}
  )
  public record GenerateFigureDescription(String prompt) {

  }

  @Prompt(
      template = """
          You are an AI writer who is trying to write a tale for the user based on their intent.
              
          When given their intent, create a complete, exhaustive summary of the story for the user.
       
          This is the prompt for the story:  {{$prompt}}
          
          And these are the persons that are part of the story:
          
          {{$persons}}
       
          respond with a full plot summary and a list of the chapters and a short description of what happens in the chapter, like:
          
          Plot: some longer description of the overall plot
          
          Chapter 1: title
          Summary: something that happens in the chapter
          
          Chapter 2: another chapter title
          Summary: something else that happens in the chapter
                    
          begin now:
                    
          """,
      variables = {"prompt", "persons"}
  )
  public record GenerateSummary(String prompt, String persons) {

  }



  @Prompt(
      template = """
          You are an AI writer who is trying to write a tale for the user based on their intent.
              
          the story is: {{$prompt}}

          the overall summary of the story: {{$summary}}

          Here is a description of persons involved in the story:
          
          {{$persons}}
              
          only write the text of the story.
          do not add any other explanation.

          We have broken up the story into per-chapter generation. 
          Now your job is to generate content for this chapter:
          
          {{$chaptercontent}}
          
          Begin generating the content for the chapter now.

          """,
      variables = {"prompt", "summary", "persons", "chaptercontent"}
  )
  public record GenerateChapter(String prompt,
                                   String summary,
                                   String persons,
                                   String chaptercontent) {

  }
}
