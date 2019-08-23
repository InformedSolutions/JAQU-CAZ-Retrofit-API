package uk.gov.caz.retrofit.model.registerjob;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import uk.gov.caz.retrofit.model.CsvContentType;

public enum RegisterJobTrigger {
  RETROFIT_CSV_FROM_S3,
  GREEN_MOD_CSV_FROM_S3,
  WHITE_MOD_CSV_FROM_S3;

  private static final Map<CsvContentType, RegisterJobTrigger> FROM_CSV_CONTENT_TYPE_MAPPING =
      ImmutableMap.of(
          CsvContentType.RETROFIT_LIST, RETROFIT_CSV_FROM_S3,
          CsvContentType.MOD_GREEN_LIST, GREEN_MOD_CSV_FROM_S3,
          CsvContentType.MOD_WHITE_LIST, WHITE_MOD_CSV_FROM_S3
      );

  /**
   * Returns {@link RegisterJobTrigger} matching {@link CsvContentType}.
   *
   * @param csvContentType {@link CsvContentType} that describes what is inside CSV file.
   * @return {@link RegisterJobTrigger} matching {@link CsvContentType}.
   * @throws UnsupportedOperationException if there is no mapping from {@link CsvContentType} to
   *     {@link RegisterJobTrigger}
   */
  public static RegisterJobTrigger from(CsvContentType csvContentType) {
    RegisterJobTrigger result = FROM_CSV_CONTENT_TYPE_MAPPING.get(csvContentType);
    if (result == null) {
      throw new UnsupportedOperationException("There is no mapping for '" + csvContentType + "'");
    }
    return result;
  }
}
