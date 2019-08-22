package uk.gov.caz.retrofit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterCsvFromS3LambdaInput {

  int registerJobId;
  String s3Bucket;
  String fileName;
  String correlationId;
  String action;
}
