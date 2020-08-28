package uk.gov.caz.retrofit.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

  private static final int DELETE_BATCH_SIZE = 10_000;

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

    deleteVehiclesInBatches(vehiclesToDelete(retrofittedVehicles));
    retrofittedVehiclePostgresRepository.insertOrUpdate(retrofittedVehicles);

    log.info("Registering {} vehicle(s) : finish", retrofittedVehicles.size());
    return RegisterResult.success();
  }

  /**
   * Method that caluclates which VRNs should be deleted from DB.
   */
  public Set<String> vehiclesToDelete(Set<RetrofittedVehicle> retrofittedVehicles) {
    Set<String> uploadedVrns = retrofittedVehicles.stream().map(RetrofittedVehicle::getVrn)
        .collect(Collectors.toSet());

    HashSet<String> existingVrns =
        new HashSet<>(retrofittedVehiclePostgresRepository.findAllVrns());

    return existingVrns.stream()
        .filter(existingVrn -> !uploadedVrns.contains(existingVrn))
        .collect(Collectors.toSet());
  }

  /**
   * Method responsible for deleting VRNs in batches.
   *
   * @param vehiclesToDelete set with all vehicles to delete.
   */
  private void deleteVehiclesInBatches(Set<String> vehiclesToDelete) {
    Iterable<List<String>> batches = Iterables.partition(vehiclesToDelete, DELETE_BATCH_SIZE);
    batches.forEach(batch -> retrofittedVehiclePostgresRepository.delete(Sets.newHashSet(batch)));
  }
}
