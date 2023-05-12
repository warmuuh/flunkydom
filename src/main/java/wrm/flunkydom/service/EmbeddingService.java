package wrm.flunkydom.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import wrm.flunkydom.persistence.Embedding;
import wrm.flunkydom.persistence.EmbeddingRepository;
import wrm.llm.tools.ChatGptFunction.OpenAiConfig;

public class EmbeddingService {

  private final EmbeddingRepository repository;
  private final Supplier<OpenAiConfig> configSupplier;

  public EmbeddingService(EmbeddingRepository repository, Supplier<OpenAiConfig> configSupplier) {
    this.configSupplier = configSupplier;
    this.repository = repository;
  }

  private OpenAiService createAiService() {
    OpenAiConfig openAiConfig = configSupplier.get();
    return new OpenAiService(openAiConfig.token());
  }


  public void updateEmbedding(String id, String title, String content) {
    float[] vector = getEmbeddingVector(content);
    Embedding createdEmbedding = new Embedding(
        id,
        title,
        content
    );
    repository.updateEmbedding(createdEmbedding, vector);
  }

  public Embedding addEmbedding(String title, String content) {
    float[] vector = getEmbeddingVector(content);
    Embedding createdEmbedding = new Embedding(
        UUID.randomUUID().toString(),
        title,
        content
    );
    repository.addNewEmbedding(createdEmbedding, vector);
    return createdEmbedding;
  }

  public List<String> fetchSimilarContentFor(String content) {
    float[] vector = getEmbeddingVector(content);

    return repository.getSimilarEmbeddings(vector, 0.7f, 3)
        .stream().map(Embedding::content).toList();
  }

  @NotNull
  private float[] getEmbeddingVector(String content) {
    OpenAiService aiService = createAiService();
    EmbeddingResult calculatedEmbedding = aiService.createEmbeddings(new EmbeddingRequest(
        "text-embedding-ada-002",
        List.of(content),
        null
    ));

    List<Double> embedding = calculatedEmbedding.getData().get(0).getEmbedding();
    float[] vector = new float[embedding.size()];
    for (int i = 0; i < embedding.size(); i++) {
      vector[i] = embedding.get(i).floatValue();
    }
    return vector;
  }

}
