package uk.gov.caz.taxiregister.service;

import com.google.common.base.Preconditions;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.model.RetrofittedVehicle;
import uk.gov.caz.taxiregister.repository.RetrofittedVehiclePostgresRepository;

/**
 * Class which is responsible for registering vehicles. It wipes all vehicles before persisting new
 * ones.
 */
@Service
@Slf4j
public class RegisterService {

  private final RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  public RegisterService(
      RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository) {
    this.retrofittedVehiclePostgresRepository = retrofittedVehiclePostgresRepository;
  }

  /**
   * Registers the passed set of {@link RetrofittedVehicle}.
   *
   * @param retrofittedVehicles A set of {@link RetrofittedVehicle} that is to be registered.
   * @return An instance of {@link RegisterResult} that represents the result of the operation.
   */
  public RegisterResult register(Set<RetrofittedVehicle> retrofittedVehicles) {
    Preconditions.checkNotNull(retrofittedVehicles, "retrofittedVehicles cannot be null");

    log.info("Registering {} vehicle(s) : start", retrofittedVehicles.size());

    retrofittedVehiclePostgresRepository.deleteAll();
    retrofittedVehiclePostgresRepository.insert(retrofittedVehicles);

    log.info("Registering {} vehicle(s) : finish", retrofittedVehicles.size());
    return RegisterResult.success();
  }
}
