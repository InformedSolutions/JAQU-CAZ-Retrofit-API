package uk.gov.caz.retrofit.model.registerjob;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.caz.retrofit.model.CsvContentType;

class RegisterJobTriggerTest {

  @Test
  public void testMappingOfAllCsvContentTypeValues() {
    // this is to quickly catch case when we add new CsvContentType but forget to update
    // RegisterJobTrigger.from method
    for (CsvContentType csvContentType : CsvContentType.values()) {
      RegisterJobTrigger.from(csvContentType);
    }
  }

  @Test
  public void testThatCorrectlyMapsFromCsvContentTypes() {
    assertThat(RegisterJobTrigger.from(CsvContentType.RETROFIT_LIST))
        .isEqualByComparingTo(RegisterJobTrigger.RETROFIT_CSV_FROM_S3);

    assertThat(RegisterJobTrigger.from(CsvContentType.MOD_GREEN_LIST))
        .isEqualByComparingTo(RegisterJobTrigger.GREEN_MOD_CSV_FROM_S3);

    assertThat(RegisterJobTrigger.from(CsvContentType.MOD_WHITE_LIST))
        .isEqualByComparingTo(RegisterJobTrigger.WHITE_MOD_CSV_FROM_S3);
  }
}