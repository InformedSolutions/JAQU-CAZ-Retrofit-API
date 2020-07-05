package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.assertj.core.api.ThrowableAssertAlternative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

@ExtendWith(MockitoExtension.class)
class RetrofitVehicleServiceTest {

  @Mock
  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  @InjectMocks
  private RetrofitVehicleService retrofitVehicleService;

  @Test
  void shouldThrowAnExceptionWhenVrnIsEmpty() {
    assertExceptionThrownForVrn("");
  }

  @Test
  void shouldThrowAnExceptionWhenVrnIsNull() {
    assertExceptionThrownForVrn(null);
  }

  private ThrowableAssertAlternative<IllegalArgumentException> assertExceptionThrownForVrn(
      String o) {
    return assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> retrofitVehicleService.existsByVrn(o))
        .withMessage("VRN cannot be empty");
  }
}