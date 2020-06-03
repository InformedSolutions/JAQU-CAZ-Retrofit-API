package uk.gov.caz.retrofit.service;

import com.google.common.base.Preconditions;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.AuditingRepository;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

/**
 * Class which is responsible for registering vehicles. It wipes all vehicles before persisting new
 * ones.
 */
@Service
@AllArgsConstructor
@Slf4j
public class RegisterService {

  private final RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  private final AuditingRepository auditingRepository;

  /**
   * Registers the passed set of {@link RetrofittedVehicle}.
   *
   * @param retrofittedVehicles A set of {@link RetrofittedVehicle} that is to be registered.
   * @return An instance of {@link RegisterResult} that represents the result of the operation.
   */
  @Transactional
  public RegisterResult register(Set<RetrofittedVehicle> retrofittedVehicles, UUID uploaderId) {
    Preconditions.checkNotNull(retrofittedVehicles, "retrofittedVehicles cannot be null");

    Preconditions.checkNotNull(uploaderId, "uploaderId cannot be null");

    log.info("Registering {} vehicle(s) : start", retrofittedVehicles.size());

    auditingRepository.tagModificationsInCurrentTransactionBy(uploaderId);
    log.info("Transaction associated with {} in the audit table.", uploaderId);

    retrofittedVehiclePostgresRepository.deleteAll();
    retrofittedVehiclePostgresRepository.insert(retrofittedVehicles);

    log.info("Registering {} vehicle(s) : finish", retrofittedVehicles.size());
    return RegisterResult.success();
  }
}
