package uk.gov.caz.taxiregister;

import org.springframework.context.annotation.Import;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;
import uk.gov.caz.taxiregister.util.DatabaseInitializer;

@IntegrationTest
@Import(DatabaseInitializer.class)
public class AuditTrailTestIT {

  //
  // TODO:
  // Uncomment and fix when we have at least one table on which we can test changes
  //

//  private static final String AUDIT_LOGGED_ACTIONS_TABLE = "audit.logged_actions";
//
//  @Autowired
//  private DatabaseInitializer databaseInitializer;
//
//  @Autowired
//  private JdbcTemplate jdbcTemplate;
//
//  @BeforeEach
//  public void init() {
//    databaseInitializer.clear();
//  }
//
//  @AfterEach
//  public void clear() {
//    databaseInitializer.clear();
//    deleteVehicleType("CustomType1");
//    deleteVehicleType("CustomType2");
//  }
//
//  @Test
//  public void testInsertUpdateDeleteOperationsAgainstAuditTrailTable() {
//    atTheBeginningAuditLoggedActionsTableShouldBeEmpty();
//
//    // INSERT case
//    whenWeInsertSomeSampleDataIntoTaxiPhvTypeTable("CustomType1",
//        "MyCustomVehicleType1Description");
//
//    thenNumberOfRowsInAuditLoggedActionsTableForTaxiPhvShouldBe(1);
//    andThereShouldBeExactlyOneInsertActionLogged();
//    withNewDataLike("(CustomType1,MyCustomVehicleType1Description");
//
//    // UPDATE case
//    whenWeUpdateTaxiPhvTypeTo("CustomType2", "CustomType1", "MyCustomVehicleType2Description");
//
//    thenNumberOfRowsInAuditLoggedActionsTableForTaxiPhvShouldBe(2);
//    andThereShouldBeExactlyOneUpdateActionLogged();
//    withNewDataLike("(CustomType2,MyCustomVehicleType2Description");
//
//    // DELETE case
//    whenWeDeleteRowFromPhvTypeTable("CustomType2");
//
//    thenNumberOfRowsInAuditLoggedActionsTableForTaxiPhvShouldBe(3);
//    andThereShouldBeExactlyOneDeleteActionLogged();
//    withNewDataEqualToNull();
//  }
//
//  private void atTheBeginningAuditLoggedActionsTableShouldBeEmpty() {
//    checkIfAuditTableContainsNumberOfRows(0);
//  }
//
//  private void whenWeInsertSomeSampleDataIntoTaxiPhvTypeTable(String vehicleType,
//      String vehicleDescription) {
//    jdbcTemplate.update(
//        "INSERT INTO public.t_md_taxi_phv_type (taxi_phv_type, taxi_phv_description) VALUES (?, ?)",
//        vehicleType, vehicleDescription);
//  }
//
//  private void thenNumberOfRowsInAuditLoggedActionsTableForTaxiPhvShouldBe(
//      int expectedNumberOfRows) {
//    checkIfAuditTableContainsNumberOfRows(expectedNumberOfRows,
//        "TABLE_NAME = 't_md_taxi_phv_type'");
//  }
//
//  private void andThereShouldBeExactlyOneInsertActionLogged() {
//    checkIfAuditTableContainsNumberOfRows(1, "action = 'I'");
//  }
//
//  private void withNewDataLike(String expectedNewData) {
//    checkIfAuditTableContainsNumberOfRows(1, "new_data like '" + expectedNewData + "%'");
//  }
//
//  private void whenWeUpdateTaxiPhvTypeTo(String newVehicleType,
//      String oldVehicleType, String newVehicleDescription) {
//    jdbcTemplate.update("UPDATE public.t_md_taxi_phv_type "
//            + "set taxi_phv_type = ?, taxi_phv_description = ? "
//            + "where taxi_phv_type = ?",
//        newVehicleType, newVehicleDescription, oldVehicleType
//    );
//  }
//
//  private void andThereShouldBeExactlyOneUpdateActionLogged() {
//    checkIfAuditTableContainsNumberOfRows(1, "action = 'U'");
//  }
//
//  private void whenWeDeleteRowFromPhvTypeTable(String vehicleType) {
//    deleteVehicleType(vehicleType);
//  }
//
//  private void deleteVehicleType(String vehicleType) {
//    jdbcTemplate.update("DELETE from public.t_md_taxi_phv_type "
//        + "where taxi_phv_type = ?", vehicleType);
//  }
//
//  private void andThereShouldBeExactlyOneDeleteActionLogged() {
//    checkIfAuditTableContainsNumberOfRows(1, "action = 'D'");
//  }
//
//  private void withNewDataEqualToNull() {
//    checkIfAuditTableContainsNumberOfRows(1, "new_data is null");
//  }
//
//  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable) {
//    int numberOfRowsInAuditTable =
//        JdbcTestUtils.countRowsInTable(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE);
//    assertThat(numberOfRowsInAuditTable)
//        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE + " table",
//            expectedNumberOfRowsInAuditTable)
//        .isEqualTo(expectedNumberOfRowsInAuditTable);
//  }
//
//  private void checkIfAuditTableContainsNumberOfRows(int expectedNumberOfRowsInAuditTable,
//      String whereClause) {
//    int numberOfRowsInAuditTable =
//        JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, AUDIT_LOGGED_ACTIONS_TABLE, whereClause);
//    assertThat(numberOfRowsInAuditTable)
//        .as("Expected %s row(s) in " + AUDIT_LOGGED_ACTIONS_TABLE
//                + " table matching where clause '%s'",
//            expectedNumberOfRowsInAuditTable, whereClause)
//        .isEqualTo(expectedNumberOfRowsInAuditTable);
//  }
}

