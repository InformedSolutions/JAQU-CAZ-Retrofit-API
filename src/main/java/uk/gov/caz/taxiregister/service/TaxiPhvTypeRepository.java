package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.taxiregister.model.VehicleType;

/**
 * A class that is responsible for getting {@link VehicleType}s from the postgres database.
 */
@Repository
@Slf4j
public class TaxiPhvTypeRepository {

  @VisibleForTesting
  static final String SELECT_ALL_SQL = "SELECT taxi_phv_type FROM t_md_taxi_phv_type";

  private static final TaxiPhvTypeRowMapper MAPPER = new TaxiPhvTypeRowMapper();

  private final JdbcTemplate jdbcTemplate;

  public TaxiPhvTypeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Returns all licence types that match {@link VehicleType} enum. If none is matching an empty
   * set is returned.
   */
  public Set<VehicleType> findAll() {
    List<VehicleType> licenceTypesList = jdbcTemplate.query(SELECT_ALL_SQL, MAPPER);
    List<VehicleType> filteredLicenceTypes = licenceTypesList.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    return filteredLicenceTypes.isEmpty()
        ? EnumSet.noneOf(VehicleType.class)
        : EnumSet.copyOf(filteredLicenceTypes);
  }

  @VisibleForTesting
  static class TaxiPhvTypeRowMapper implements RowMapper<VehicleType> {
    @Override
    public VehicleType mapRow(ResultSet rs, int i) throws SQLException {
      String taxiPhvType = rs.getString("taxi_phv_type");
      try {
        return VehicleType.valueOf(taxiPhvType);
      } catch (IllegalArgumentException e) {
        log.warn("'{}' is not a valid licence type, returning null", taxiPhvType);
      }
      return null;
    }
  }
}
