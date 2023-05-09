package wrm.flunkydom.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
        row.getInt("steps")
    );
  }

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }


  public void addNewGoal(Goal goal) {
    jdbcTemplate.update("Insert into goals(goal_id, input_query, creation_time, status) VALUES (?,?,?,?);",
        goal.id(),
        goal.inputQuery(),
        Timestamp.from(goal.creationTime()),
        goal.status()
    );
  }

  public void updateGoal(Goal goal) {
    jdbcTemplate.update("Update goals set finish_time=?, status=?, result=?, steps=?, log=? where goal_id = ?;",
        goal.finishTime() != null ? Timestamp.from(goal.finishTime()) : null,
        goal.status(),
        goal.result(),
        goal.steps(),
        goal.log(),
        goal.id()
    );
  }

}
