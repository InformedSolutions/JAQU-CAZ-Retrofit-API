package uk.gov.caz.taxiregister.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.dto.VehicleDto;

@Service
public class SourceAwareRegisterService {

  private final RegisterCommandFactory registerCommandFactory;


  public SourceAwareRegisterService(RegisterCommandFactory registerCommandFactory) {
    this.registerCommandFactory = registerCommandFactory;
  }

  /**
   * Registers vehicles whose data is located at S3 in bucket {@code bucket} and key {@code
   * filename}.
   *
   * @param bucket The name of the bucket at S3 where files with vehicles data is stored
   * @param filename The name of the key at S3 where vehicles data is stored
   * @param registerJobId Uniquely identifies Register Job
   * @param correlationId Identifier of one particular request flow
   */
  public RegisterResult register(String bucket, String filename, int registerJobId,
      String correlationId) {
    RegisterFromCsvCommand command = registerCommandFactory
        .createRegisterFromCsvCommand(bucket, filename, registerJobId, correlationId);
    return command.execute();
  }

  /**
   * Registers vehicles with data from direct API call.
   *
   * @param licences List of vehicles/licences taken from the API caller
   * @param uploaderId Uploader ID
   * @param registerJobId Uniquely identifies Register Job
   * @param correlationId Identifier of one particular request flow
   */
  public RegisterResult register(List<VehicleDto> licences, UUID uploaderId, int registerJobId,
      String correlationId) {
    RegisterFromRestApiCommand command = registerCommandFactory
        .createRegisterFromRestApiCommand(licences, registerJobId, correlationId, uploaderId);
    return command.execute();
  }
}
