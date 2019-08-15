package uk.gov.caz.taxiregister.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncJavaBackgroundJobStarterTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String CSV_FILE = "fileName";

  @Mock
  private SourceAwareRegisterService mockedService;

  @InjectMocks
  private AsyncJavaBackgroundJobStarter javaJobStarter;

  @Test
  public void asyncJavaBackgroundJobStarterStartsBackgroundJobWithCorrectParameters() {
    // given

    // when
    javaJobStarter
        .fireAndForgetRegisterCsvFromS3Job(S3_REGISTER_JOB_ID, S3_BUCKET, CSV_FILE,
            TYPICAL_CORRELATION_ID);

    // then
    Awaitility
        .await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> verify(mockedService)
                .register(S3_BUCKET, CSV_FILE, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID));
    verifyNoMoreInteractions(mockedService);
  }
}