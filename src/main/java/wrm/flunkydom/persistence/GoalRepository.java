package wrm.flunkydom.persistence;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import wrm.flunkydom.persistence.Goal.Artifact;

@Repository
public class GoalRepository {

  private final JdbcTemplate jdbcTemplate;


  public GoalRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Goal findById(String id) {
    return jdbcTemplate.queryForObject("Select * from goals where goal_id = ?",
        new Object[]{id},
        (row, idx) -> mapRowToGoal(row));
  }

  public void deleteById(String id) {
    jdbcTemplate.update("delete from goals where goal_id = ?",
        id);
  }

  public List<Goal> findAll() {
    return jdbcTemplate.query("Select * from goals",
        (row, idx) -> mapRowToGoal(row));
  }

  @NotNull
  private static Goal mapRowToGoal(ResultSet row) throws SQLException {
    return new Goal(
        row.getString("goal_id"),
        row.getString("input_query"),
        toInstant(row.getTimestamp("creation_time")),
        toInstant(row.getTimestamp("finish_time")),
        row.getString("status"),
        row.getString("result"),
        row.getString("log"),
        row.getInt("steps"),
        row.getString("agent"),
        readZipFile(row.getBytes("artifacts"))
    );
  }

  private static List<Artifact> readZipFile(byte[] artifacts) {
    if (artifacts == null || artifacts.length == 0) {
      return new LinkedList<>();
    }
    List<Artifact> entries = new ArrayList<>();
    byte[] buffer = new byte[2048];

    try (ZipInputStream zi = new ZipInputStream(new ByteArrayInputStream(artifacts))) {

      ZipEntry zipEntry = null;
      while ((zipEntry = zi.getNextEntry()) != null) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (BufferedOutputStream bos = new BufferedOutputStream(byteArrayOutputStream, buffer.length)) {
          int len;
          while ((len = zi.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
          }
        }
        entries.add(new Artifact(zipEntry.getName(), byteArrayOutputStream.toByteArray()));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return entries;
  }

  private byte[] writeZipFile(List<Artifact> artifacts) {
    if (artifacts.isEmpty()) {
      return null;
    }
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try (ZipOutputStream zo = new ZipOutputStream(byteArrayOutputStream)) {

      for (Artifact artifact : artifacts) {
        ZipEntry e = new ZipEntry(artifact.filename());
        zo.putNextEntry(e);
        zo.write(artifact.content());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return byteArrayOutputStream.toByteArray();
  }

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }


  public void addNewGoal(Goal goal) {
    jdbcTemplate.update("Insert into goals(goal_id, input_query, creation_time, status, agent) VALUES (?,?,?,?,?);",
        goal.id(),
        goal.inputQuery(),
        Timestamp.from(goal.creationTime()),
        goal.status(),
        goal.agent()
    );
  }

  public void updateGoal(Goal goal) {
    jdbcTemplate.update("Update goals set finish_time=?, status=?, result=?, steps=?, log=?, artifacts=? where goal_id = ?;",
        goal.finishTime() != null ? Timestamp.from(goal.finishTime()) : null,
        goal.status(),
        goal.result(),
        goal.steps(),
        goal.log(),
        writeZipFile(goal.artifacts()),
        goal.id()
    );
  }

}
