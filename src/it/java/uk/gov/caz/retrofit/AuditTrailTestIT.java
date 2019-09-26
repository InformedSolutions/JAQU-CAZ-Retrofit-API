package uk.gov.caz.retrofit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;
import uk.gov.caz.retrofit.annotation.IntegrationTest;
import uk.gov.caz.testutils.TestObjects;

@IntegrationTest
@Sql(scripts = {
    "classpath:data/sql/clear.sql",
    "classpath:data/sql/add-audit-log-data.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/delete-audit-log-data.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class AuditTrailTestIT {

  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "audit.logged_actions";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  public void testInsertUpdateDeleteOperationsAgainstAuditTrailTable() {
    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();

    // INSERT case
    whenWeInsertSomeSampleDataIntoRegisterJobsTable("InitialJobName");
    thenNumberOfRowsInAuditLoggedActionsTableForRegisterJobsShouldBe(1);
    andThereShouldBeExactlyOneInsertActionLogged();
    withNewDataLike("RETROFIT_CSV_FROM_S3", "InitialJobName",
        "11111111-2222-3333-4444-555555555555", "RUNNING");

    // UPDATE case
    whenWeUpdateRegisterJobsTo("InitialJobName", "ModifiedJobName");

    thenNumberOfRowsInAuditLoggedActionsTableForRegisterJobsShouldBe(2);
    andThereShouldBeExactlyOneUpdateActionLogged();
    withNewDataLike("RETROFIT_CSV_FROM_S3", "ModifiedJobName",
        "11111111-2222-3333-4444-555555555555", "RUNNING");

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
        "INSERT INTO table_for_audit_test(TRIGGER, JOB_NAME, UPLOADER_ID, STATUS) "
            + "VALUES (?, ?, ?, ?)", TestObjects.S3_RETROFIT_REGISTER_JOB_TRIGGER.name(),
        jobName, TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID, "RUNNING");
  }

  private void thenNumberOfRowsInAuditLoggedActionsTableForRegisterJobsShouldBe(
      int expectedNumberOfRows) {
    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
        "TABLE_NAME = 'table_for_audit_test'");
  }

  private void andThereShouldBeExactlyOneInsertActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'I'");
  }

  private void withNewDataLike(String trigger, String jobName, String uploaderId, String status) {
    checkIfAuditTableContainsNumberOfRows(1,
        "'" + toJson(trigger, jobName, uploaderId, status) + "'::jsonb @> new_data");
  }

  private void whenWeUpdateRegisterJobsTo(String oldJobName, String newJobName) {
    jdbcTemplate.update("UPDATE table_for_audit_test "
            + "set JOB_NAME = ? "
            + "where JOB_NAME = ?",
        newJobName, oldJobName
    );
  }

  private void andThereShouldBeExactlyOneUpdateActionLogged() {
    checkIfAuditTableContainsNumberOfRows(1, "action = 'U'");
  }

  private void whenWeDeleteRowFromRegisterJobsTable(String jobName) {
    jdbcTemplate.update("DELETE from table_for_audit_test "
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

  @SneakyThrows
  private String toJson(String trigger, String jobName, String uploaderId,
      String status) {
    return objectMapper.writeValueAsString(ImmutableMap.of(
        "trigger", trigger,
        "job_name", jobName,
        "uploader_id", uploaderId,
        "status", status
    ));
  }
}

