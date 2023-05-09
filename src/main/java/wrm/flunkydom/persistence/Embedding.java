package wrm.flunkydom.persistence;

import com.pgvector.PGvector;
import java.time.Instant;

public record Embedding(
    String id,
    String title,
    String content
    ){

}
