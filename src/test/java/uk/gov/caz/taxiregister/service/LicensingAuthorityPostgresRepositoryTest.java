package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.service.LicensingAuthorityPostgresRepository.LicensingAuthorityRowMapper;

@ExtendWith(MockitoExtension.class)
class LicensingAuthorityPostgresRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private LicensingAuthorityPostgresRepository licensingAuthorityPostgresRepository;

  @Test
  public void shouldFindAllLicensingAuthorities() {
    List<LicensingAuthority> licensingAuthorities = mockDataInDatabase();

    Map<String, LicensingAuthority> result = licensingAuthorityPostgresRepository.findAll();

    assertThat(result.values()).containsExactlyElementsOf(licensingAuthorities);
  }

  @Nested
  class RowMapper {

    private LicensingAuthorityRowMapper rowMapper = new LicensingAuthorityRowMapper();

    @Test
    public void shouldMapResultSetToLicensingAuthorityWithAnyValidValues() throws SQLException {
      String name = "la-name-1";
      int id = 1;
      ResultSet resultSet = mockResultSetWithAnyValidValues(name, id);

      LicensingAuthority licensingAuthority = rowMapper.mapRow(resultSet, 0);

      assertThat(licensingAuthority).isNotNull();
      assertThat(licensingAuthority.getName()).isEqualTo(name);
      assertThat(licensingAuthority.getId()).isEqualTo(id);
    }

    private ResultSet mockResultSetWithAnyValidValues(String name, int id) throws SQLException {
      ResultSet resultSet = mock(ResultSet.class);
      when(resultSet.getString("licence_authority_name")).thenReturn(name);
      when(resultSet.getInt("licence_authority_id")).thenReturn(id);
      return resultSet;
    }
  }

  private List<LicensingAuthority> mockDataInDatabase() {
    List<LicensingAuthority> licensingAuthorities = Collections.singletonList(
        new LicensingAuthority(1, "la-1")
    );
    when(jdbcTemplate.query(anyString(), any(LicensingAuthorityRowMapper.class))).thenReturn(licensingAuthorities);
    return licensingAuthorities;
  }
}