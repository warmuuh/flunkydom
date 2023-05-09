package wrm.flunkydom.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ToolRepository {

  private final JdbcTemplate jdbcTemplate;


  public ToolRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public ToolConfiguration findById(String id) {
    return jdbcTemplate.queryForObject("Select * from tool_cfgs where id = ?",
        new Object[]{id},
        (row, idx) -> mapRowToToolConfiguration(row));
  }

  public void deleteById(String id) {
    jdbcTemplate.update("delete from tool_cfgs where id = ?",
        id);
  }

  public List<ToolConfiguration> findAll() {
    return jdbcTemplate.query("Select * from tool_cfgs",
        (row, idx) -> mapRowToToolConfiguration(row));
  }

  @NotNull
  private static ToolConfiguration mapRowToToolConfiguration(ResultSet row) throws SQLException {
    return new ToolConfiguration(
        row.getString("id"),
        row.getString("config_json")
    );
  }

  public void addNewToolConfiguration(ToolConfiguration tool) {
    jdbcTemplate.update("Insert into tool_cfgs(id, config_json) VALUES (?,?);",
        tool.toolId(),
        tool.configJson()
    );
  }

  public void updateToolConfiguration(ToolConfiguration tool) {
    jdbcTemplate.update("Update tool_cfgs set config_json=? where id = ?;",
        tool.configJson(),
        tool.toolId()
    );
  }

}
