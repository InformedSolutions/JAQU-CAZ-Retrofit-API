package uk.gov.caz.taxiregister.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

  private static final List<Path> REGISTER_JOB_DATA = Collections.singletonList(
      createTestSqlDataPath("register-job-data.sql")
  );

  public DatabaseInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
    this.jdbcTemplate = jdbcTemplate;
    this.dataSource = dataSource;
  }

  public void init() throws Exception {
  }

  public void initRegisterJobData() throws Exception {
    executeScripts(REGISTER_JOB_DATA);
  }

  public void clear() {
    jdbcTemplate.execute("TRUNCATE TABLE retrofit.T_MD_REGISTER_JOBS CASCADE");
    jdbcTemplate.execute("TRUNCATE TABLE audit.logged_actions CASCADE");
    jdbcTemplate.execute("TRUNCATE TABLE retrofit.t_vehicle_retrofit CASCADE");
  }

  private void executeScripts(List<Path> scripts) throws Exception {
    for (Path script : scripts) {
      FileSystemResource resource = new FileSystemResource(script);
      ScriptUtils.executeSqlScript(dataSource.getConnection(), resource);
    }
  }

  private static Path createTestSqlDataPath(String s) {
    return Paths.get("src", "it", "resources", "data", "sql", s);
  }
}
