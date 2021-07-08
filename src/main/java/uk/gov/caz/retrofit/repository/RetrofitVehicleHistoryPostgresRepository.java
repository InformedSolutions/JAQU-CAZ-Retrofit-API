package uk.gov.caz.retrofit.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import uk.gov.caz.retrofit.dto.RetrofitVehicleHistoricalInfo;
import uk.gov.caz.retrofit.dto.RetrofitVehicleHistory;

/**
 * A class that is responsible for managing vehicle's licences historical data ({@link
 * uk.gov.caz.retrofit.dto.RetrofitVehicleHistoricalInfo} entities) in the postgres database.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RetrofitVehicleHistoryPostgresRepository {

  private static final ZoneId LONDON_ZONE_ID = ZoneId.of("Europe/London");
  private static final VehicleHistoryRowMapper MAPPER = new VehicleHistoryRowMapper();

  @VisibleForTesting
  protected static final ImmutableMap<String, String> EXPECTED_ACTION_VALUES =
      new ImmutableMap.Builder<String, String>()
          .put("U", "Updated")
          .put("I", "Created")
          .put("D", "Removed")
          .build();

  private static final String SELECT_FIELDS = "SELECT "
      + "a.action as action, "
      + "a.action_tstamp as action_tstamp, "
      + "a.new_data::json ->> 'vrn' as new_vrn, "
      + "a.new_data::json ->> 'vehicle_category' as new_vehicle_category, "
      + "a.new_data::json ->> 'model' as new_model, "
      + "a.new_data::json ->> 'date_of_retrofit' as new_date_of_retrofit, "
      + "a.original_data::json ->> 'vrn' as original_vrn, "
      + "a.original_data::json ->> 'vehicle_category' as original_category, "
      + "a.original_data::json ->> 'model' as original_model, "
      + "a.original_data::json ->> 'date_of_retrofit' as original_date_of_retrofit ";

  private static final String NEW_DATA_QUERY_SUFFIX = "FROM audit.logged_actions a "
      + "WHERE (a.new_data::json ->> 'vrn' = ?) "
      + "AND a.action_tstamp >= ? "
      + "AND a.action_tstamp <= ? "
      + "AND table_name = 't_vehicle_retrofit' ";

  private static final String ORIGINAL_DATA_QUERY_SUFFIX = "FROM audit.logged_actions a "
      + "WHERE (a.original_data::json ->> 'vrn' = ?) "
      + "AND a.action_tstamp >= ? "
      + "AND a.action_tstamp <= ? "
      + "AND table_name = 't_vehicle_retrofit' ";

  private static final String PAGING_SUFFIX = "ORDER BY action_tstamp DESC "
      + "LIMIT ? "
      + "OFFSET ? ";

  private static final String SELECT_BY_VRN_HISTORY_UNION_QUERY =
      "(" + SELECT_FIELDS + NEW_DATA_QUERY_SUFFIX + ")"
          + " UNION ALL "
          + "(" + SELECT_FIELDS + ORIGINAL_DATA_QUERY_SUFFIX + ") "
          + PAGING_SUFFIX;

  static final String SELECT_BY_VRN_HISTORY_IN_RANGE_COUNT = "SELECT COUNT(action_tstamp) FROM ( "
      + "(SELECT a.action_tstamp " + NEW_DATA_QUERY_SUFFIX + ")"
      + " UNION ALL "
      + "(SELECT a.action_tstamp " + ORIGINAL_DATA_QUERY_SUFFIX + ") "
      + ") as compound_result";

  private final JdbcTemplate jdbcTemplate;

  /**
   * Finds all {@link RetrofitVehicleHistoricalInfo} entities for a given vrn and date range.
   *
   * @param vrn for which all matching licences are returned
   * @return {@link List} of all {@link RetrofitVehicleHistory}.
   */
  public List<RetrofitVehicleHistory> findByVrnInRange(String vrn, LocalDateTime startDate,
      LocalDateTime endDate, long pageSize, long pageNumber) {
    return jdbcTemplate.query(
        SELECT_BY_VRN_HISTORY_UNION_QUERY,
        preparedStatement -> {
          int i = 0;
          preparedStatement.setString(++i, vrn);
          preparedStatement.setObject(++i, startDate);
          preparedStatement.setObject(++i, endDate);
          preparedStatement.setString(++i, vrn);
          preparedStatement.setObject(++i, startDate);
          preparedStatement.setObject(++i, endDate);
          preparedStatement.setObject(++i, pageSize);
          preparedStatement.setObject(++i, pageNumber * pageSize);
        },
        MAPPER
    );
  }

  /**
   * Count all {@link RetrofitVehicleHistoricalInfo} entities for a given vrn and date range.
   *
   * @param vrn for which all matching licences are returned
   * @return {@link Long} of all histories which matches passed vrn and date range.
   */
  public Long count(String vrn, LocalDateTime startDate, LocalDateTime endDate) {
    List<Object> ts = Arrays.asList(vrn, startDate, endDate, vrn, startDate, endDate);
    return jdbcTemplate.queryForObject(
        SELECT_BY_VRN_HISTORY_IN_RANGE_COUNT,
        ts.toArray(),
        Long.class
    );
  }

  @VisibleForTesting
  static class VehicleHistoryRowMapper implements RowMapper<RetrofitVehicleHistory> {

    private static final String DELETE_ACTION = "D";

    @Override
    public RetrofitVehicleHistory mapRow(ResultSet rs, int i) throws SQLException {
      String action = rs.getString("action");
      boolean isRemoved = DELETE_ACTION.equals(action);
      return RetrofitVehicleHistory.builder()
          .modifyDate(Optional.ofNullable(rs.getObject("action_tstamp", OffsetDateTime.class))
              .map(offsetDateTime -> offsetDateTime.atZoneSameInstant(LONDON_ZONE_ID).toLocalDate())
              .orElse(null))
          .action(mapAction(action))
          .vehicleCategory(
              rs.getString(isRemoved ? "original_category" : "new_vehicle_category"))
          .model(
              rs.getString(isRemoved ? "original_model" : "new_model"))
          .dateOfRetrofit(
              Optional.ofNullable(
                  rs.getDate(isRemoved
                      ? "original_date_of_retrofit"
                      : "new_date_of_retrofit").toLocalDate()
              ).orElse(null))
          .build();
    }

    static String mapAction(String wheelchairAccessFlag) {
      return EXPECTED_ACTION_VALUES.getOrDefault(wheelchairAccessFlag, null);
    }
  }
}
