package wrm.llm.agent;

import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessor;
import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessorConfig;
import ai.knowly.langtoch.llm.schema.io.SingleText;
import ai.knowly.langtoch.prompt.annotation.Prompt;
import ai.knowly.langtoch.prompt.annotation.PromptProcessor;
import com.theokanning.openai.service.OpenAiService;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.logging.log4j.util.Strings;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import wrm.llm.tools.ChatGptFunction.OpenAiConfig;
import wrm.llm.tools.Tool.ToolOutcome;

/**
 * based on https://github.com/smol-ai/developer
 */
public class SmolDevAgent extends Agent {

  private final Supplier<OpenAiConfig> aiConfigSupplier;

  public SmolDevAgent(Supplier<OpenAiConfig> aiConfigSupplier) {
    super("smoldev");
    this.aiConfigSupplier = aiConfigSupplier;
  }

  @Override
  public AgentTask createInitialTask(String parentId, String query) {
    var currentPrompt = PromptProcessor.createPromptTemplate(GenerateFilepaths.class,
            new GenerateFilepaths(query))
        .format();
    System.out.print(currentPrompt);
    AgentTask currentTask = new AgentTask(parentId, 0, currentPrompt, Optional.empty(), Map.of(
        "state", "genfilepaths",
        "originalPrompt", query
    ));
    return currentTask;
  }


  @Override
  public NextTask iterateOnTask(AgentTask task) {
    OpenAITextProcessor aiTextProcessor = createTextProcessor();
    String state = task.customData().get("state");
    String query = task.customData().get("originalPrompt");

    if (state.equals("genfilepaths")) {
      var filepaths = aiTextProcessor.run(SingleText.of(task.prompt())).getText().trim().replace(" ", "");

      var sharedDepPrompt = PromptProcessor.createPromptTemplate(GenerateSharedDependencies.class,
              new GenerateSharedDependencies(query, filepaths))
          .format();

      return new NextTask(new AgentTask(task.parentId(), 1, sharedDepPrompt, Optional.empty(), Map.of(
          "state", "shareddependencies",
          "originalPrompt", query,
          "filepaths", filepaths,
          "generateFiles", filepaths
      )), Optional.empty());
    } else if (state.equals("shareddependencies")) {
      var sharedDependencies = aiTextProcessor.run(SingleText.of(task.prompt())).getText();

      String[] filesToGenerate = task.customData().get("generateFiles").split(",");

      if (filesToGenerate.length > 0) {
        var generateFilePrompt = PromptProcessor.createPromptTemplate(GenerateFilePrompt.class,
                new GenerateFilePrompt(query, task.customData().get("filepaths"), sharedDependencies, filesToGenerate[0], filesToGenerate[0], query))
            .format();

        return new NextTask(new AgentTask(task.parentId(), 2000 + filesToGenerate.length,
            generateFilePrompt, Optional.empty(), Map.of(
            "state", "genfile",
            "originalPrompt", query,
            "filepaths", task.customData().get("filepaths"),
            "sharedDependencies", sharedDependencies,
            "currentFile", filesToGenerate[0],
            "generateFiles", String.join(",", Arrays.copyOfRange(filesToGenerate, 1, filesToGenerate.length))
        )), Optional.empty());
      }
    } else if (state.equals("genfile")) {
      String currentFile = task.customData().get("currentFile").trim();
      String sharedDependencies = task.customData().get("sharedDependencies");
      String[] filesToGenerate =  task.customData().get("generateFiles").isEmpty()
          ? new String[]{}
          : task.customData().get("generateFiles").split(",");
      String filepaths = task.customData().get("filepaths");

      if (Strings.isNotBlank(currentFile)){
        var fileContent = aiTextProcessor.run(SingleText.of(task.prompt())).getText();

        Path generatedFilePath = Paths.get(System.getProperty("user.home"), "generated", currentFile);
        generatedFilePath.getParent().toFile().mkdirs();
        try {
          BufferedWriter writer = new BufferedWriter(new FileWriter(generatedFilePath.toFile()));
          writer.write(fileContent);
          writer.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      if (filesToGenerate.length > 0) {
        var generateFilePrompt = PromptProcessor.createPromptTemplate(GenerateFilePrompt.class,
                new GenerateFilePrompt(query, filepaths, sharedDependencies, filesToGenerate[0], filesToGenerate[0], query))
            .format();

        String[] nextFiles = Arrays.copyOfRange(filesToGenerate, 1, filesToGenerate.length);
        return new NextTask(new AgentTask(task.parentId(), 2000 + filesToGenerate.length,
            generateFilePrompt, Optional.empty(), Map.of(
            "state", "genfile",
            "originalPrompt", query,
            "filepaths", filepaths,
            "sharedDependencies", sharedDependencies,
            "currentFile", filesToGenerate[0],
            "generateFiles", String.join(",", nextFiles)
        )), Optional.empty());
      } else {
        return new NextTask(new AgentTask(task.parentId(), 2000,
            "", Optional.of("generated to " + Paths.get(System.getProperty("user.home"), "generated")), Map.of()),
            Optional.empty());
      }
    }

    throw new IllegalStateException("Unknown state: "  + state);
  }




  private OpenAITextProcessor createTextProcessor() {
    OpenAiConfig openAiConfig = aiConfigSupplier.get();
    var aiTextProcessor = OpenAITextProcessor.create(new OpenAiService(openAiConfig.token()));
    aiTextProcessor.withConfig(OpenAITextProcessorConfig.builder()
        .setModel("text-davinci-003")
        .setMaxTokens(2048)
        .setTemperature(0.0)
        .setMaxTokens(256)
        .setStream(false)
        .setStop(List.of("APSOJD:")) // empty list breaks
        .build());
    return aiTextProcessor;
  }


  @Prompt(
      template = """
          You are an AI developer who is trying to write a program that will generate code for the user based on their intent.
              
          When given their intent, create a complete, exhaustive list of filepaths that the user would write to make the program.
            
          only list the filepaths you would write, and return them as a comma-separated list of strings.
                    do not add any other explanation, only return a comma-separated list of strings.
                    
          {{$prompt}}
          
          """,
      variables = {"prompt"}
  )
  public record GenerateFilepaths(String prompt) {

  }

  @Prompt(
      template = """
          You are an AI developer who is trying to write a program that will generate code for the user based on their intent.
                    
          In response to the user's prompt:
                  
          ---
          the app is: {{$prompt}}
          ---
                   
          the files we have decided to generate are: {{$filepaths}}
                  
          Now that we have a list of files, we need to understand what dependencies they share.
          Please name and briefly describe what is shared between the files we are generating, including exported variables, data schemas, id names of every DOM elements that javascript functions will use, message names, and function names.
          Exclusively focus on the names of the shared dependencies, and do not add any other explanation.
                   
          """,
      variables = {"prompt", "filepaths"}
  )
  public record GenerateSharedDependencies(String prompt, String filepaths) {

  }

  @Prompt(
      template = """
          You are an AI developer who is trying to write a program that will generate code for the user based on their intent.
              
          the app is: {{$prompt}}

          the files we have decided to generate are: {{$filepaths}}

          the shared dependencies (like filenames and variable names) we have decided on are: {{$sharedDependencies}}
              
          only write valid code for the given filepath and file type, and return only the code.
          do not add any other explanation, only return valid code for that file type.

          We have broken up the program into per-file generation. 
          Now your job is to generate only the code for the file {{$filename1}}. 
          Make sure to have consistent filenames if you reference other files we are also generating.
              
          Remember that you must obey 3 things: 
             - you are generating code for the file {{$filename2}}
             - do not stray from the names of the files and the shared dependencies we have decided on
             - MOST IMPORTANT OF ALL - the purpose of our app is {{$prompt2}} - every line of code you generate must be valid code. Do not include code fences in your response, for example
              
          Bad response:
          ```javascript 
          console.log("hello world")
          ```
              
          Good response:
          console.log("hello world")
              
          Begin generating the code now.

          """,
      variables = {"prompt", "filepaths", "sharedDependencies", "filename1", "filename2", "prompt2"}
  )
  public record GenerateFilePrompt(String prompt,
                                   String filepaths,
                                   String sharedDependencies,
                                   String filename1,
                                   String filename2,
                                   String prompt2) {

  }
}
