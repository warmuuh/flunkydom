package wrm.flunkydom.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import wrm.flunkydom.AgentConfiguration;

@Repository
public class AgentRepository {

  private final JdbcTemplate jdbcTemplate;


  public AgentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public AgentConfig findById(String id) {
    return jdbcTemplate.queryForObject("Select * from agents where id = ?",
        new Object[]{id},
        (row, idx) -> mapRowToAgentConfiguration(row));
  }

  public void deleteById(String id) {
    jdbcTemplate.update("delete from agents where id = ?",
        id);
  }

  public List<AgentConfig> findAll() {
    return jdbcTemplate.query("Select * from agents",
        (row, idx) -> mapRowToAgentConfiguration(row));
  }

  @NotNull
  private static AgentConfig mapRowToAgentConfiguration(ResultSet row) throws SQLException {
    return new AgentConfig(
        row.getString("id"),
        row.getString("name"),
        row.getString("agent_template"),
        List.of(row.getString("active_tools").split(","))
    );
  }

  public void addNewAgentConfiguration(AgentConfig agent) {
    jdbcTemplate.update("Insert into agents(id, name, agent_template, active_tools) VALUES (?,?,?,?);",
        agent.id(),
        agent.name(),
        agent.agentTemplateId(),
        String.join(",", agent.activeTools())
    );
  }

  public void updateAgentConfiguration(AgentConfig agent) {
    jdbcTemplate.update("Update agents set name=?, agent_template=?, active_tools=? where id = ?;",
        agent.name(),
        agent.agentTemplateId(),
        String.join(",", agent.activeTools()),
        agent.id()
    );
  }

}
