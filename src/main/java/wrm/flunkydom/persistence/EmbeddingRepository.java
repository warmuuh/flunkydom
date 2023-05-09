package wrm.flunkydom.persistence;

import com.pgvector.PGvector;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;

@Repository
public class EmbeddingRepository {

  private final JdbcTemplate jdbcTemplate;


  public EmbeddingRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Embedding findById(String id) {
    return jdbcTemplate.queryForObject("Select * from embeddings where id = ?",
        new Object[]{id},
        (row, idx) -> mapRowToEmbedding(row));
  }

  public void deleteById(String id) {
    jdbcTemplate.update("delete from embeddings where id = ?",
        id);
  }

  public List<Embedding> findAll() {
    return jdbcTemplate.query("Select * from embeddings",
        (row, idx) -> mapRowToEmbedding(row));
  }

  @NotNull
  private static Embedding mapRowToEmbedding(ResultSet row) throws SQLException {
    return new Embedding(
        row.getString("id"),
        row.getString("title"),
        row.getString("content")
    );
  }


  public void addNewEmbedding(Embedding embedding, float[] embeddingVector) {
    jdbcTemplate.update("Insert into embeddings(id, title, content, embedding) VALUES (?,?,?,?);",
        embedding.id(),
        embedding.title(),
        embedding.content(),
        new PGvector(embeddingVector)
    );
  }

  public void updateEmbedding(Embedding embedding, float[] embeddingVector) {
    jdbcTemplate.update("Update embeddings set title=?, content=?, embedding=? where id = ?",
        embedding.title(),
        embedding.content(),
        new PGvector(embeddingVector),
        embedding.id()
    );
  }

  public List<Embedding> getSimilarEmbeddings(float[] embedding, float minSimilarity, int limit) {
    return jdbcTemplate.query("""
            select
                embeddings.id,
                embeddings.title,
                embeddings.content
            from embeddings
            where 1 - (embeddings.embedding <=> ?) > ?
            limit ?""",
        new Object[]{new PGvector(embedding), minSimilarity, limit},
        (row, idx) -> mapRowToEmbedding(row));
  }
}
