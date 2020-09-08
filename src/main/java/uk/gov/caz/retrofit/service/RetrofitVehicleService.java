package uk.gov.caz.retrofit.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

/**
 * Class responsible for managing data for retrofitted vehicles.
 */
@Service
@AllArgsConstructor
public class RetrofitVehicleService {

  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  public boolean existsByVrn(String vrn) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vrn), "VRN cannot be empty");
    return retrofittedVehiclePostgresRepository.existsByVrn(vrn);
  }
}
