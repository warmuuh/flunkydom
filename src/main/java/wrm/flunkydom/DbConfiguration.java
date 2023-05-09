package wrm.flunkydom;

import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessor;
import ai.knowly.langtoch.llm.processor.openai.text.OpenAITextProcessorConfig;
import ai.knowly.langtoch.tool.Tool;
import com.pgvector.PGvector;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import wrm.llm.agent.Agent;

@Configuration
public class DbConfiguration {

  @Autowired
  JdbcTemplate jdbcTemplate;

  @PostConstruct
  void setupConnection() {
    DataSource dataSource = jdbcTemplate.getDataSource();
    jdbcTemplate.setDataSource(new DelegatingDataSource(dataSource) {
      @Override
      public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        PGvector.addVectorType(connection);
        return connection;
      }
    });
  }

}
