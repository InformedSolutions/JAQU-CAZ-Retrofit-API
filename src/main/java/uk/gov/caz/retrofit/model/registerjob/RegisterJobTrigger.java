package uk.gov.caz.retrofit.model.registerjob;

import uk.gov.caz.retrofit.model.CsvContentType;

public enum RegisterJobTrigger {
  RETROFIT_CSV_FROM_S3,
  GREEN_MOD_CSV_FROM_S3,
  WHITE_MOD_CSV_FROM_S3;

  /**
   * Returns {@link RegisterJobTrigger} matching {@link CsvContentType}.
   *
   * @param csvContentType {@link CsvContentType} that describes what is inside CSV file.
   * @return {@link RegisterJobTrigger} matching {@link CsvContentType}.
   */
  public static RegisterJobTrigger from(CsvContentType csvContentType) {
    switch (csvContentType) {
      case RETROFIT_LIST:
        return RETROFIT_CSV_FROM_S3;
      case MOD_GREEN_LIST:
        return GREEN_MOD_CSV_FROM_S3;
      case MOD_WHITE_LIST:
        return WHITE_MOD_CSV_FROM_S3;
      default:
        throw new UnsupportedOperationException(
            "There is no mapping for '" + csvContentType.name() + "'");
    }
  }
}
