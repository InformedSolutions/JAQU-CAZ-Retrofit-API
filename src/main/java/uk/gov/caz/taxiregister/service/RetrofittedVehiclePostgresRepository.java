package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.caz.taxiregister.model.RetrofittedVehicle;

@Slf4j
@Repository
public class RetrofittedVehiclePostgresRepository {

  @VisibleForTesting
  static final String DELETE_ALL_SQL = "DELETE FROM retrofit.t_md_retroffited_vehicles";

  @VisibleForTesting
  static final String INSERT_SQL = "INSERT INTO retrofit.t_md_retroffited_vehicles("
      + "vrn, "
      + "vehicle_category, "
      + "model, "
      + "date_of_retrofit, "
      + "insert_timestmp) "
      + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";


  private final JdbcTemplate jdbcTemplate;
  private final int updateBatchSize;

  public RetrofittedVehiclePostgresRepository(JdbcTemplate jdbcTemplate,
      @Value("${application.jdbc.updateBatchSize:100}") int updateBatchSize) {
    this.jdbcTemplate = jdbcTemplate;
    this.updateBatchSize = updateBatchSize;
  }

  public void insert(Set<RetrofittedVehicle> retrofittedVehicles) {
    Iterable<List<RetrofittedVehicle>> batches = Iterables
        .partition(retrofittedVehicles, updateBatchSize);
    for (List<RetrofittedVehicle> batch : batches) {
      jdbcTemplate.batchUpdate(INSERT_SQL, new InsertBatchPreparedStatementSetter(batch));
    }
  }

  public void deleteAll() {
    log.info("Deleting all retrofitted vehicles.");
    jdbcTemplate.update(DELETE_ALL_SQL);
  }

  public List<RetrofittedVehicle> findAll() {
    return new LinkedList<>();
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
  }

  private static int setInsertStatementAttributes(PreparedStatement preparedStatement,
      RetrofittedVehicle retrofittedVehicle) throws SQLException {
    int i = 0;
    preparedStatement.setString(++i, retrofittedVehicle.getVrn());
    preparedStatement.setString(++i, retrofittedVehicle.getVehicleCategory());
    preparedStatement.setString(++i, retrofittedVehicle.getModel());
    preparedStatement.setObject(++i, retrofittedVehicle.getDateOfRetrofitInstallation());
    return i;
  }
}