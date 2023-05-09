package wrm.flunkydom.service;

import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import wrm.flunkydom.persistence.Embedding;
import wrm.flunkydom.persistence.EmbeddingRepository;

public class EmbeddingService {

  private final OpenAiService aiService;
  private final EmbeddingRepository repository;

  public EmbeddingService(OpenAiService aiService, EmbeddingRepository repository) {
    this.aiService = aiService;
    this.repository = repository;
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
