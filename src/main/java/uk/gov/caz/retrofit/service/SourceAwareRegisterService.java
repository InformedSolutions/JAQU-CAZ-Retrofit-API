package uk.gov.caz.retrofit.service;

import org.springframework.stereotype.Service;

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
}
