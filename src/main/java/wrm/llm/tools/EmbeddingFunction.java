package wrm.llm.tools;

import java.util.Optional;

public class EmbeddingFunction extends Tool<Void> {

  private final EmbeddingProvider embeddingProvider;

  public EmbeddingFunction(EmbeddingProvider embeddingProvider) {
    super("knowledge", """
        a knowledge base that can retrieve information for any topics. use this with HIGHEST priority when you need more information""");
    this.embeddingProvider = embeddingProvider;
  }

  @Override
  public String getToolConfigId() {
    return "openai";
  }

  @Override
  public ToolOutcome execute(String input, Void configuration) {
    String foundEmbedding = embeddingProvider.findSimilarEmbeddingFor(input);
    if (foundEmbedding == null) {
      return new ToolOutcome("nothing found", Optional.empty());
    }
    return new ToolOutcome(foundEmbedding, Optional.empty());
  }

  @FunctionalInterface
  public interface EmbeddingProvider {

    String findSimilarEmbeddingFor(String input);
  }
}
