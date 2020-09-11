package uk.gov.caz.retrofit.repository;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;

@Slf4j
@Repository
public class RetrofittedVehiclePostgresRepository {

  private static final String FIND_ALL_SQL = "SELECT * FROM t_vehicle_retrofit";

  private static final String FIND_ALL_VRNS_SQL = "SELECT vrn FROM t_vehicle_retrofit";

  private static final String COUNT_BY_VRN =
      "SELECT count(*) FROM t_vehicle_retrofit WHERE vrn = ?";

  @VisibleForTesting
  static final String DELETE_ALL_SQL = "DELETE FROM t_vehicle_retrofit";

  @VisibleForTesting
  static final String INSERT_SQL = "INSERT INTO t_vehicle_retrofit as d ("
      + "vrn, "
      + "vehicle_category, "
      + "model, "
      + "date_of_retrofit, "
      + "insert_timestmp) "
      + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) "
      + "ON CONFLICT (vrn) "
      + "DO UPDATE SET "
      + "vehicle_category = excluded.vehicle_category, "
      + "model = excluded.model, "
      + "date_of_retrofit = excluded.date_of_retrofit "
      + "where d.vehicle_category != excluded.vehicle_category "
      + "or d.model != excluded.model "
      + "or d.date_of_retrofit != excluded.date_of_retrofit";

  static final String CREATE_TEMP_TABLE =
      "CREATE TEMP TABLE IF NOT EXISTS vrns_to_delete_tmp (id varchar(15))";
  static final String INSERT_VRNS_INTO_TEMP_TABLE = "INSERT INTO vrns_to_delete_tmp VALUES(?)";
  static final String DELETE_TEMP_TABLE_VRNS =
      "DELETE FROM t_vehicle_retrofit WHERE vrn IN (SELECT id FROM vrns_to_delete_tmp)";
  static final String DROP_TEMP_TABLE = "DROP TABLE vrns_to_delete_tmp";

  private final JdbcTemplate jdbcTemplate;
  private final int updateBatchSize;

  /**
   * Public constructor that is used by Spring to initialize this class.
   */
  public RetrofittedVehiclePostgresRepository(JdbcTemplate jdbcTemplate,
      @Value("${application.jdbc.updateBatchSize:100}") int updateBatchSize) {
    this.jdbcTemplate = jdbcTemplate;
    this.updateBatchSize = updateBatchSize;
  }

  /**
   * Inserts passed set of {@link RetrofittedVehicle} into the database in batches. The size of a
   * single batch sits in {@code application.jdbc.updateBatchSize} application property.
   *
   * @param retrofittedVehicles A set of vehicles that will be inserted in the database.
   */
  public void insertOrUpdate(Set<RetrofittedVehicle> retrofittedVehicles) {
    Iterable<List<RetrofittedVehicle>> batches = Iterables
        .partition(retrofittedVehicles, updateBatchSize);
    for (List<RetrofittedVehicle> batch : batches) {
      jdbcTemplate.batchUpdate(INSERT_SQL, new InsertBatchPreparedStatementSetter(batch));
    }
  }

  /**
   * Deletes all records from {@code t_vehicle_retrofit} table.
   */
  public void deleteAll() {
    log.info("Deleting all retrofitted vehicles.");
    jdbcTemplate.update(DELETE_ALL_SQL);
  }

  /**
   * Deletes records from {@code t_vehicle_retrofit} table by its VRN value.
   */
  public void delete(Set<String> vrns) {
    if (vrns.isEmpty()) {
      return;
    }

    log.info("Deleting retroffited vehicles");

    List<Object[]> vrnsList = vrns.stream().map(vrn -> new Object[]{vrn})
        .collect(Collectors.toList());

    jdbcTemplate.execute(CREATE_TEMP_TABLE);
    jdbcTemplate.batchUpdate(INSERT_VRNS_INTO_TEMP_TABLE, vrnsList);
    jdbcTemplate.update(DELETE_TEMP_TABLE_VRNS);
    jdbcTemplate.execute(DROP_TEMP_TABLE);
  }

  /**
   * Finds whether vehicle for given vrn exists in DB.
   */
  public boolean existsByVrn(String vrn) {
    return jdbcTemplate.queryForObject(COUNT_BY_VRN, new Object[]{vrn}, Integer.class) > 0;
  }

  /**
   * Returns list of all vrns stored in {@code t_vehicle_retrofit} table.
   */
  public List<String> findAllVrns() {
    return jdbcTemplate.query(FIND_ALL_VRNS_SQL,
        (resultSet, i) -> resultSet.getString("vrn"));
  }

  /**
   * Finds all vehicles in the database.
   *
   * @return A list of all vehicles in {@code t_vehicle_retrofit} table.
   */
  @VisibleForTesting
  public List<RetrofittedVehicle> findAll() {
    return jdbcTemplate.query(FIND_ALL_SQL, (rs, rowNum) -> RetrofittedVehicle.builder()
        .vrn(rs.getString("vrn"))
        .vehicleCategory(rs.getString("vehicle_category"))
        .model(rs.getString("model"))
        .dateOfRetrofitInstallation(rs.getObject("date_of_retrofit", LocalDate.class))
        .build());
  }

  @VisibleForTesting
  static class InsertBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private final List<RetrofittedVehicle> batch;

    InsertBatchPreparedStatementSetter(List<RetrofittedVehicle> batch) {
      this.batch = batch;
    }

    @Override
    public void setValues(PreparedStatement preparedStatement, int index) throws SQLException {
      RetrofittedVehicle retrofittedVehicle = batch.get(index);
      setInsertStatementAttributes(preparedStatement, retrofittedVehicle);
    }

    @Override
    public int getBatchSize() {
      return batch.size();
    }

    private int setInsertStatementAttributes(PreparedStatement preparedStatement,
        RetrofittedVehicle retrofittedVehicle) throws SQLException {
      int i = 0;
      preparedStatement.setString(++i, retrofittedVehicle.getVrn());
      preparedStatement.setString(++i, retrofittedVehicle.getVehicleCategory());
      preparedStatement.setString(++i, retrofittedVehicle.getModel());
      preparedStatement.setObject(++i, retrofittedVehicle.getDateOfRetrofitInstallation());
      return i;
    }
  }
}
