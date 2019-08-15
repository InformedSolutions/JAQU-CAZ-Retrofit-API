package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.taxiregister.model.LicensingAuthority;

/**
 * A class that manages access to {@link LicensingAuthority} entities in database.
 */
@Repository
public class LicensingAuthorityPostgresRepository {

  private static final String SELECT_ALL_SQL = "select "
      + "licence_authority_id, "
      + "licence_authority_name "
      + "from t_md_licensing_authority";
  private static final RowMapper<LicensingAuthority> MAPPER = new LicensingAuthorityRowMapper();

  private final JdbcTemplate jdbcTemplate;

  /**
   * Creates an instance of {@link LicensingAuthorityPostgresRepository}.
   *
   * @param jdbcTemplate An instance of {@link JdbcTemplate}
   */
  public LicensingAuthorityPostgresRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Finds all {@link LicensingAuthority} entities in the database. Returns them in a map that
   * assigns licensing authority name to {@link LicensingAuthority}
   *
   * @return {@link Map} containing all {@link LicensingAuthority} entities in the database. The map
   *     assigns licensing authority name to {@link LicensingAuthority}
   */
  public Map<String, LicensingAuthority> findAll() {
    List<LicensingAuthority> licensingAuthorities = jdbcTemplate.query(SELECT_ALL_SQL, MAPPER);
    return licensingAuthorities.stream()
        .collect(Collectors.toMap(LicensingAuthority::getName, Function.identity()));
  }

  @VisibleForTesting
  static class LicensingAuthorityRowMapper implements RowMapper<LicensingAuthority> {
    @Override
    public LicensingAuthority mapRow(ResultSet rs, int i) throws SQLException {
      return new LicensingAuthority(
          rs.getInt("licence_authority_id"),
          rs.getString("licence_authority_name")
      );
    }
  }
}
