package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.taxiregister.service.TaxiPhvTypeRepository.SELECT_ALL_SQL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.taxiregister.model.VehicleType;
import uk.gov.caz.taxiregister.service.TaxiPhvTypeRepository.TaxiPhvTypeRowMapper;

@ExtendWith(MockitoExtension.class)
class TaxiPhvTypeRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private TaxiPhvTypeRepository taxiPhvTypeRepository;

  @Test
  public void shouldReturnEmptySetWhenThereIsNoMatchingType() {
    // given
    List<VehicleType> convertedVehicleTypes = Arrays.asList(null, null, null);
    given(jdbcTemplate.query(eq(SELECT_ALL_SQL), any(TaxiPhvTypeRowMapper.class)))
        .willReturn(convertedVehicleTypes);

    // when
    Set<VehicleType> vehicleTypes = taxiPhvTypeRepository.findAll();

    // then
    then(vehicleTypes).isEmpty();
  }

  @Test
  public void shouldReturnPhvTypeWhenItIsReturnedWithNullValues() {
    // given
    List<VehicleType> convertedVehicleTypes = Arrays.asList(null, null, VehicleType.PHV, null);
    given(jdbcTemplate.query(eq(SELECT_ALL_SQL), any(TaxiPhvTypeRowMapper.class)))
        .willReturn(convertedVehicleTypes);

    // when
    Set<VehicleType> vehicleTypes = taxiPhvTypeRepository.findAll();

    // then
    then(vehicleTypes).containsExactly(VehicleType.PHV);
  }

  @Test
  public void shouldReturnPhvTypeWhenItIsReturned() {
    // given
    List<VehicleType> convertedVehicleTypes = Collections.singletonList(VehicleType.PHV);
    given(jdbcTemplate.query(eq(SELECT_ALL_SQL), any(TaxiPhvTypeRowMapper.class)))
        .willReturn(convertedVehicleTypes);

    // when
    Set<VehicleType> vehicleTypes = taxiPhvTypeRepository.findAll();

    // then
    then(vehicleTypes).containsExactly(VehicleType.PHV);
  }

  @Test
  public void shouldReturnTaxiTypeWhenItIsReturnedWithNullValues() {
    // given
    List<VehicleType> convertedVehicleTypes = Arrays.asList(null, null, VehicleType.TAXI, null);
    given(jdbcTemplate.query(eq(SELECT_ALL_SQL), any(TaxiPhvTypeRowMapper.class)))
        .willReturn(convertedVehicleTypes);

    // when
    Set<VehicleType> vehicleTypes = taxiPhvTypeRepository.findAll();

    // then
    then(vehicleTypes).containsExactly(VehicleType.TAXI);
  }

  @Test
  public void shouldReturnTaxiTypeWhenItIsReturned() {
    // given
    List<VehicleType> convertedVehicleTypes = Collections.singletonList(VehicleType.TAXI);
    given(jdbcTemplate.query(eq(SELECT_ALL_SQL), any(TaxiPhvTypeRowMapper.class)))
        .willReturn(convertedVehicleTypes);

    // when
    Set<VehicleType> vehicleTypes = taxiPhvTypeRepository.findAll();

    // then
    then(vehicleTypes).containsExactly(VehicleType.TAXI);
  }

  @Test
  public void shouldReturnTaxiAndPhvTypesWhenTheyAreReturnedWithNullValues() {
    // given
    List<VehicleType> convertedVehicleTypes = Arrays.asList(VehicleType.PHV, null, null, VehicleType.TAXI, null);
    given(jdbcTemplate.query(eq(SELECT_ALL_SQL), any(TaxiPhvTypeRowMapper.class)))
        .willReturn(convertedVehicleTypes);

    // when
    Set<VehicleType> vehicleTypes = taxiPhvTypeRepository.findAll();

    // then
    then(vehicleTypes).containsExactlyInAnyOrder(VehicleType.TAXI, VehicleType.PHV);
  }

  @Test
  public void shouldReturnTaxiAndPhvTypesWhenTheyAreReturned() {
    // given
    List<VehicleType> convertedVehicleTypes = Arrays.asList(VehicleType.TAXI, VehicleType.PHV);
    given(jdbcTemplate.query(eq(SELECT_ALL_SQL), any(TaxiPhvTypeRowMapper.class)))
        .willReturn(convertedVehicleTypes);

    // when
    Set<VehicleType> vehicleTypes = taxiPhvTypeRepository.findAll();

    // then
    then(vehicleTypes).containsExactlyInAnyOrder(VehicleType.TAXI, VehicleType.PHV);
  }

  @Nested
  class RowMapper {
    private TaxiPhvTypeRowMapper mapper = new TaxiPhvTypeRowMapper();

    @Mock
    private ResultSet resultSet;

    @Test
    public void shouldReturnNullWhenValueInDbDoesNotMatchEnum() throws SQLException {
      // given
      given(resultSet.getString("taxi_phv_type")).willReturn("unknown");

      // when
      VehicleType vehicleType = mapper.mapRow(resultSet, 0);

      // then
      then(vehicleType).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"TAXI", "PHV"})
    public void shouldReturnValidValueWhenValueInDbMatchesEnum(String valueInDb) throws SQLException {
      // given
      given(resultSet.getString("taxi_phv_type")).willReturn(valueInDb);

      // when
      VehicleType vehicleType = mapper.mapRow(resultSet, 0);

      // then
      then(vehicleType).isEqualTo(VehicleType.valueOf(valueInDb));
    }
  }
}