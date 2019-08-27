package uk.gov.caz.retrofit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.retrofit.annotation.IntegrationTest;
import uk.gov.caz.testutils.TestObjects;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class AuditTrailTestIT {

  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "audit.logged_actions";
  private static final LocalDate DATE = LocalDate.of(2019, 8, 14);

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void testInsertUpdateDeleteOperationsAgainstAuditTrailTable() {
    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();

    // INSERT case
    whenWeInsertSomeSampleDataIntoRegisterJobsTable("InitialJobName");
    thenNumberOfRowsInAuditLoggedActionsTableForRegisterJobsShouldBe(1);
    andThereShouldBeExactlyOneInsertActionLogged();
    withNewDataLike(
        "(1,RETROFIT_CSV_FROM_S3,InitialJobName,11111111-2222-3333-4444-555555555555,RUNNING,,CorrelationId,\"2019-08-14 00:00:00\",\"2019-08-14 00:00:00\")");

    // UPDATE case
    whenWeUpdateRegisterJobsTo("InitialJobName", "ModifiedJobName");

    thenNumberOfRowsInAuditLoggedActionsTableForRegisterJobsShouldBe(2);
    andThereShouldBeExactlyOneUpdateActionLogged();
    withNewDataLike(
        "(1,RETROFIT_CSV_FROM_S3,ModifiedJobName,11111111-2222-3333-4444-555555555555,RUNNING,,CorrelationId,\"2019-08-14 00:00:00\",\"2019-08-14 00:00:00\")");

    // DELETE case
    whenWeDeleteRowFromRegisterJobsTable("ModifiedJobName");

    thenNumberOfRowsInAuditLoggedActionsTableForRegisterJobsShouldBe(3);
    andThereShouldBeExactlyOneDeleteActionLogged();
    withNewDataEqualToNull();
  }

  private void atTheBeginningAuditLoggedActionsTableShouldBeEmpty() {
    checkIfAuditTableContainsNumberOfRows(0);
  }

  private void whenWeInsertSomeSampleDataIntoRegisterJobsTable(String jobName) {
    jdbcTemplate.update(
        "INSERT INTO t_md_register_jobs(REGISTER_JOB_ID, TRIGGER, JOB_NAME, UPLOADER_ID, STATUS, "
            + "CORRELATION_ID, INSERT_TIMESTMP, LAST_MODIFIED_TIMESTMP) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        1, TestObjects.S3_RETROFIT_REGISTER_JOB_TRIGGER.name(), jobName,
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID, "RUNNING", TestObjects.TYPICAL_CORRELATION_ID,
        DATE, DATE);
  }

  private void thenNumberOfRowsInAuditLoggedActionsTableForRegisterJobsShouldBe(
      int expectedNumberOfRows) {
    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
        "TABLE_NAME = 't_md_register_jobs'");
  }

  private void andThereShouldBeExactlyOneInsertActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'I'");
  }

  private void withNewDataLike(String expectedNewData) {
    checkIfAuditTableContainsNumberOfRows(1, "new_data like '" + expectedNewData + "%'");
  }

  private void whenWeUpdateRegisterJobsTo(String oldJobName, String newJobName) {
    jdbcTemplate.update("UPDATE t_md_register_jobs "
            + "set JOB_NAME = ? "
            + "where JOB_NAME = ?",
        newJobName, oldJobName
    );
  }

  private void andThereShouldBeExactlyOneUpdateActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'U'");
  }

  private void whenWeDeleteRowFromRegisterJobsTable(String jobName) {
    jdbcTemplate.update("DELETE from t_md_register_jobs "
        + "where JOB_NAME = ?", jobName);
  }

  private void andThereShouldBeExactlyOneDeleteActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'D'");
  }

  private void withNewDataEqualToNull() {
    checkIfAuditTableContainsNumberOfRows(1, "new_data is null");
  }

  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTable(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE + " table",
            expectedNumberOfRowsInAuditTable)
        .isEqualTo(expectedNumberOfRowsInAuditTable);
  }

  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable,
      String whereClause) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE, whereClause);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE
                + " table matching where clause '%s'",
            expectedNumberOfRowsInAuditTable, whereClause)
        .isEqualTo(expectedNumberOfRowsInAuditTable);
  }
}

