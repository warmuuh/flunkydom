package wrm.flunkydom;

import com.pgvector.PGvector;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;

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
