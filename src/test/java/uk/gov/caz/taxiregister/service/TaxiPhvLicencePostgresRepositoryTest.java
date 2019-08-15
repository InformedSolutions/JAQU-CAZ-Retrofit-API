package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.VehicleType;
import uk.gov.caz.taxiregister.service.TaxiPhvLicencePostgresRepository.DeleteBatchPreparedStatementSetter;
import uk.gov.caz.taxiregister.service.TaxiPhvLicencePostgresRepository.InsertBatchPreparedStatementSetter;
import uk.gov.caz.taxiregister.service.TaxiPhvLicencePostgresRepository.LicenceRowMapper;
import uk.gov.caz.taxiregister.service.TaxiPhvLicencePostgresRepository.UpdateBatchPreparedStatementSetter;

@ExtendWith(MockitoExtension.class)
class TaxiPhvLicencePostgresRepositoryTest {

  private static final int ANY_BATCH_SIZE = 2;

  private static final UUID ANY_UPLOADER_ID = UUID.randomUUID();

  private static final String ANY_VRM = "8839GF";

  private static final TaxiPhvVehicleLicence ANY_TAXI_PHV_VEHICLE_LICENCE = TaxiPhvVehicleLicence
      .builder()
      .id(1)
      .uploaderId(ANY_UPLOADER_ID)
      .vrm(ANY_VRM)
      .wheelchairAccessible(true)
      .licensePlateNumber("plate")
      .vehicleType(VehicleType.TAXI)
      .licenseDates(new LicenseDates(LocalDate.now(), LocalDate.now().plusDays(1)))
      .licensingAuthority(
          new LicensingAuthority(1, "la-name")
      )
      .build();

  private static final Set<TaxiPhvVehicleLicence> ANY_TAXI_PHV_VEHICLE_LICENCES = ImmutableSet.of(
      ANY_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .id(1)
          .vrm("1289J")
          .build(),
      ANY_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .id(2)
          .vrm("K97LUK")
          .build(),
      ANY_TAXI_PHV_VEHICLE_LICENCE.toBuilder()
          .id(3)
          .vrm("AAA999")
          .build()
  );

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Captor
  private ArgumentCaptor<PreparedStatementSetter> preparedStmtSetterArgumentCaptor;

  private TaxiPhvLicencePostgresRepository taxiPhvLicencePostgresRepository;

  @BeforeEach
  public void init() {
    taxiPhvLicencePostgresRepository = new TaxiPhvLicencePostgresRepository(
        jdbcTemplate,
        ANY_BATCH_SIZE,
        ANY_BATCH_SIZE
    );
  }

  @Test
  public void shouldFindAllVehicles() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = mockDataInDatabaseForFindAll();

    List<TaxiPhvVehicleLicence> result = taxiPhvLicencePostgresRepository.findAll();

    assertThat(result).containsExactlyElementsOf(taxiPhvVehicleLicences);
  }

  @Test
  public void shouldFindAllVehiclesForGivenUploader() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = mockDataInDatabaseForVrm();

    List<TaxiPhvVehicleLicence> result = taxiPhvLicencePostgresRepository.findByVrm(ANY_VRM);

    assertThat(result).containsExactlyElementsOf(taxiPhvVehicleLicences);
  }

  @Test
  public void shouldFindAllVehiclesForGivenVrm() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = mockDataInDatabaseForUploader();

    Set<TaxiPhvVehicleLicence> result = taxiPhvLicencePostgresRepository
        .findByUploaderId(ANY_UPLOADER_ID);

    assertThat(result).containsExactlyElementsOf(taxiPhvVehicleLicences);
  }

  @Test
  public void shouldSetUploaderIdInPreparedStatementForGivenUploader() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    mockDataInDatabaseForUploader();

    taxiPhvLicencePostgresRepository.findByUploaderId(ANY_UPLOADER_ID);
    PreparedStatementSetter value = preparedStmtSetterArgumentCaptor.getValue();
    value.setValues(preparedStatement);

    verify(preparedStatement).setObject(1, ANY_UPLOADER_ID);
  }

  @Test
  public void shouldSetUploaderIdInPreparedStatementForGivenVrm() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    mockDataInDatabaseForVrm();

    taxiPhvLicencePostgresRepository.findByVrm(ANY_VRM);
    PreparedStatementSetter value = preparedStmtSetterArgumentCaptor.getValue();
    value.setValues(preparedStatement);

    verify(preparedStatement).setString(1, ANY_VRM);
  }

  @Test
  public void shouldUpdateInBatches() {
    Set<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = ANY_TAXI_PHV_VEHICLE_LICENCES;

    taxiPhvLicencePostgresRepository.update(taxiPhvVehicleLicences);

    verify(jdbcTemplate, times(2))
        .batchUpdate(eq(TaxiPhvLicencePostgresRepository.UPDATE_SQL),
            any(BatchPreparedStatementSetter.class));
  }

  @Test
  public void shouldInsertInBatches() {
    Set<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = ANY_TAXI_PHV_VEHICLE_LICENCES;

    taxiPhvLicencePostgresRepository.insert(taxiPhvVehicleLicences);

    verify(jdbcTemplate, times(2))
        .batchUpdate(eq(TaxiPhvLicencePostgresRepository.INSERT_SQL),
            any(BatchPreparedStatementSetter.class));
  }

  @Test
  public void shouldDeleteInBatches() {
    Set<Integer> vehicles = ANY_TAXI_PHV_VEHICLE_LICENCES.stream().map(TaxiPhvVehicleLicence::getId)
        .collect(Collectors.toSet());

    taxiPhvLicencePostgresRepository.delete(vehicles);

    verify(jdbcTemplate, times(2))
        .batchUpdate(
            eq(TaxiPhvLicencePostgresRepository.DELETE_SQL),
            any(BatchPreparedStatementSetter.class)
        );
  }

  @Nested
  class RowMapper {

    private LicenceRowMapper rowMapper = new LicenceRowMapper();

    @Test
    public void shouldMapResultSetToVehicleWithWheelchairAccessToTrueWhenFlagSetToY() throws SQLException {
      ResultSet resultSet = mockResultSetForWheelchairAccessFlagSetTo("y");

      TaxiPhvVehicleLicence taxiPhvVehicleLicence = rowMapper.mapRow(resultSet, 0);

      assertThat(taxiPhvVehicleLicence.getWheelchairAccessible()).isTrue();
    }

    @Test
    public void shouldMapResultSetToVehicleWithWheelchairAccessToFalseWhenFlagSetToN() throws SQLException {
      ResultSet resultSet = mockResultSetForWheelchairAccessFlagSetTo("n");

      TaxiPhvVehicleLicence taxiPhvVehicleLicence = rowMapper.mapRow(resultSet, 0);

      assertThat(taxiPhvVehicleLicence.getWheelchairAccessible()).isFalse();
    }

    @Test
    public void shouldMapResultSetToVehicleWithWheelchairAccessToNullWhenFlagSetToNull() throws SQLException {
      ResultSet resultSet = mockResultSetForWheelchairAccessFlagSetTo(null);

      TaxiPhvVehicleLicence taxiPhvVehicleLicence = rowMapper.mapRow(resultSet, 0);

      assertThat(taxiPhvVehicleLicence.getWheelchairAccessible()).isNull();
    }

    @Test
    public void shouldMapResultSetToVehicleWithTaxiPhvTypeSetToTaxi() throws SQLException {
      ResultSet resultSet = mockResultSetForTaxiPhvTypeSetTo("TAXI");

      TaxiPhvVehicleLicence taxiPhvVehicleLicence = rowMapper.mapRow(resultSet, 0);

      assertThat(taxiPhvVehicleLicence.getVehicleType()).isSameAs(VehicleType.TAXI);
    }

    @Test
    public void shouldMapResultSetToVehicleWithTaxiPhvTypeSetToPhv() throws SQLException {
      ResultSet resultSet = mockResultSetForTaxiPhvTypeSetTo("PHV");

      TaxiPhvVehicleLicence taxiPhvVehicleLicence = rowMapper.mapRow(resultSet, 0);

      assertThat(taxiPhvVehicleLicence.getVehicleType()).isSameAs(VehicleType.PHV);
    }

    @Test
    public void shouldMapResultSetToVehicleWithAnyValidValues() throws SQLException {
      ResultSet resultSet = mockResultSetWithAnyValidValues();

      TaxiPhvVehicleLicence taxiPhvVehicleLicence = rowMapper.mapRow(resultSet, 0);

      assertThat(taxiPhvVehicleLicence).isNotNull();
      assertThat(taxiPhvVehicleLicence.getUploaderId()).isNotNull();
      assertThat(taxiPhvVehicleLicence.getId()).isNotNull();
      assertThat(taxiPhvVehicleLicence.getWheelchairAccessible()).isTrue();
    }

    private ResultSet mockResultSetWithAnyValidValues() throws SQLException {
      return mockResultSet("y", "TAXI");
    }

    private ResultSet mockResultSetForWheelchairAccessFlagSetTo(String n) throws SQLException {
      return mockResultSet(n, "TAXI");
    }

    private ResultSet mockResultSetForTaxiPhvTypeSetTo(String taxiPhvType) throws SQLException {
      return mockResultSet("y", taxiPhvType);
    }

    private ResultSet mockResultSet(String wheelchairAccessFlagValue, String taxiPhvType)
        throws SQLException {
      ResultSet resultSet = mock(ResultSet.class);

      when(resultSet.getInt(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case "taxi_phv_register_id":
            return 1;
          case "licence_authority_id":
            return 999;
        }
        throw new RuntimeException("Value not stubbed!");
      });

      when(resultSet.getObject(anyString(), (Class<?>) any(Class.class))).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        Class<?> clazz = answer.getArgument(1);
        if (LocalDate.class.equals(clazz)) {
          if ("licence_start_date".equals(argument)) {
            return LocalDate.now();
          } else {
            return LocalDate.now().plusDays(1);
          }
        }
        return ANY_UPLOADER_ID;
      });

      when(resultSet.getString(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case "vrm":
            return "AAA99";
          case "licence_authority_name":
            return "la-name-1";
          case "licence_plate_number":
            return "plate-1";
          case "taxi_phv_type":
            return taxiPhvType;
          case "wheelchair_access_flag":
            return wheelchairAccessFlagValue;
        }
        throw new RuntimeException("Value not stubbed!");
      });
      return resultSet;
    }
  }

  @Nested
  class InsertPreparedStatementSetter {

    @Test
    public void shouldSetPreparedStatementAttributes() throws SQLException {
      TaxiPhvVehicleLicence taxiPhvVehicleLicence = ANY_TAXI_PHV_VEHICLE_LICENCE;
      PreparedStatement preparedStatement = mock(PreparedStatement.class);

      InsertBatchPreparedStatementSetter setter = createSetter(Collections.singletonList(
          taxiPhvVehicleLicence));
      setter.setValues(preparedStatement, 0);

      verify(preparedStatement).setString(1, taxiPhvVehicleLicence.getVrm());
      verify(preparedStatement).setString(2, taxiPhvVehicleLicence.getVehicleType().name());
      verify(preparedStatement).setObject(3, taxiPhvVehicleLicence.getLicenseDates().getStart());
      verify(preparedStatement).setObject(4, taxiPhvVehicleLicence.getLicenseDates().getEnd());
      verify(preparedStatement).setInt(5, taxiPhvVehicleLicence.getLicensingAuthority().getId());
      verify(preparedStatement)
          .setString(6, taxiPhvVehicleLicence.getLicensePlateNumber());
      verify(preparedStatement)
          .setString(7, Optional.ofNullable(taxiPhvVehicleLicence.getWheelchairAccessible())
              .map(wheelchair -> wheelchair ? "y" : "n")
              .orElse(null));
      verify(preparedStatement).setObject(8, taxiPhvVehicleLicence.getUploaderId());
    }

    @Test
    public void shouldSetPreparedStatementAttributeWhenWheelchairFlagIsFalse() throws SQLException {
      TaxiPhvVehicleLicence taxiPhvVehicleLicence = ANY_TAXI_PHV_VEHICLE_LICENCE
          .toBuilder()
          .wheelchairAccessible(false)
          .build();
      PreparedStatement preparedStatement = mock(PreparedStatement.class);

      InsertBatchPreparedStatementSetter setter = createSetter(Collections.singletonList(
          taxiPhvVehicleLicence));
      setter.setValues(preparedStatement, 0);

      verify(preparedStatement).setString(7, "n");
    }

    @Test
    public void shouldReturnBatchSize() {
      List<TaxiPhvVehicleLicence> input = Collections.singletonList(ANY_TAXI_PHV_VEHICLE_LICENCE);
      InsertBatchPreparedStatementSetter setter = createSetter(input);

      int batchSize = setter.getBatchSize();

      assertThat(batchSize).isEqualTo(input.size());
    }

    private InsertBatchPreparedStatementSetter createSetter(
        List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences) {
      return new InsertBatchPreparedStatementSetter(taxiPhvVehicleLicences);
    }
  }

  @Nested
  class UpdatePreparedStatementSetter {

    @Test
    public void shouldSetPreparedStatementAttributes() throws SQLException {
      TaxiPhvVehicleLicence taxiPhvVehicleLicence = ANY_TAXI_PHV_VEHICLE_LICENCE;
      PreparedStatement preparedStatement = mock(PreparedStatement.class);

      UpdateBatchPreparedStatementSetter setter = new UpdateBatchPreparedStatementSetter(
          Collections.singletonList(taxiPhvVehicleLicence)
      );
      setter.setValues(preparedStatement, 0);

      verify(preparedStatement).setString(1, taxiPhvVehicleLicence.getVrm());
      verify(preparedStatement).setString(2, taxiPhvVehicleLicence.getVehicleType().name());
      verify(preparedStatement).setObject(3, taxiPhvVehicleLicence.getLicenseDates().getStart());
      verify(preparedStatement).setObject(4, taxiPhvVehicleLicence.getLicenseDates().getEnd());
      verify(preparedStatement).setInt(5, taxiPhvVehicleLicence.getLicensingAuthority().getId());
      verify(preparedStatement)
          .setString(6, taxiPhvVehicleLicence.getLicensePlateNumber());
      verify(preparedStatement)
          .setString(7, taxiPhvVehicleLicence.getWheelchairAccessible() ? "y" : "n");
      verify(preparedStatement).setObject(8, taxiPhvVehicleLicence.getUploaderId());
      verify(preparedStatement).setObject(9, taxiPhvVehicleLicence.getId());
    }

    @Test
    public void shouldReturnBatchSize() {
      List<TaxiPhvVehicleLicence> input = Collections.singletonList(ANY_TAXI_PHV_VEHICLE_LICENCE);
      UpdateBatchPreparedStatementSetter setter = new UpdateBatchPreparedStatementSetter(input);

      int batchSize = setter.getBatchSize();

      assertThat(batchSize).isEqualTo(input.size());
    }
  }

  @Nested
  class DeletePreparedStatementSetter {

    @Test
    public void shouldSetPreparedStatementAttributes() throws SQLException {
      List<Integer> batch = Arrays.asList(3, 4, 5);
      PreparedStatement preparedStatement = mock(PreparedStatement.class);
      DeleteBatchPreparedStatementSetter setter = new DeleteBatchPreparedStatementSetter(batch);

      setter.setValues(preparedStatement, 0);
      setter.setValues(preparedStatement, 1);
      setter.setValues(preparedStatement, 2);

      verify(preparedStatement).setInt(1, batch.get(0));
      verify(preparedStatement).setInt(1, batch.get(1));
      verify(preparedStatement).setInt(1, batch.get(2));
    }

    @Test
    public void shouldReturnBatchSize() {
      List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
      DeleteBatchPreparedStatementSetter setter = new DeleteBatchPreparedStatementSetter(input);

      int batchSize = setter.getBatchSize();

      assertThat(batchSize).isEqualTo(input.size());
    }
  }

  private List<TaxiPhvVehicleLicence> mockDataInDatabaseForUploader() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = Collections.singletonList(
        ANY_TAXI_PHV_VEHICLE_LICENCE);
    when(jdbcTemplate.query(
        eq(TaxiPhvLicencePostgresRepository.SELECT_BY_UPLOADER_ID_SQL),
        preparedStmtSetterArgumentCaptor.capture(),
        any(LicenceRowMapper.class)
    )).thenReturn(taxiPhvVehicleLicences);
    return taxiPhvVehicleLicences;
  }

  private List<TaxiPhvVehicleLicence> mockDataInDatabaseForVrm() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = Collections.singletonList(
        ANY_TAXI_PHV_VEHICLE_LICENCE);
    when(jdbcTemplate.query(
        eq(TaxiPhvLicencePostgresRepository.SELECT_BY_VRM_SQL),
        preparedStmtSetterArgumentCaptor.capture(),
        any(LicenceRowMapper.class)
    )).thenReturn(taxiPhvVehicleLicences);
    return taxiPhvVehicleLicences;
  }

  private List<TaxiPhvVehicleLicence> mockDataInDatabaseForFindAll() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = Collections.singletonList(
        ANY_TAXI_PHV_VEHICLE_LICENCE);
    when(jdbcTemplate.query(
        eq(TaxiPhvLicencePostgresRepository.SELECT_ALL_SQL),
        any(LicenceRowMapper.class))
    ).thenReturn(taxiPhvVehicleLicences);
    return taxiPhvVehicleLicences;
  }
}