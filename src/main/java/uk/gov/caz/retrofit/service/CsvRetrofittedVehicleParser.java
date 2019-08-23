package uk.gov.caz.retrofit.service;

import com.opencsv.ICSVParser;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;
import uk.gov.caz.retrofit.service.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.retrofit.service.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.retrofit.service.exception.CsvMaxLineLengthExceededException;

public class CsvRetrofittedVehicleParser implements ICSVParser {

  static final int MAX_LINE_LENGTH = 100;
  static final int MIN_FIELDS_CNT = 2;
  static final int MAX_FIELDS_CNT = 4;

  private static final String MAX_LENGTH_MESSAGE_TEMPLATE =
      "Line is too long (max:" + CsvRetrofittedVehicleParser.MAX_LINE_LENGTH + ", current: %d).";
  private static final String LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE = "Line contains %d fields "
      + "whereas it should between " + MIN_FIELDS_CNT + " and " + MAX_FIELDS_CNT + ".";

  private static final String REGEX = "^[\\w &,'\"\\-().*/%!+:;=?@\\[\\]^{}~]+$";
  private static final Pattern ALLOWABLE_CHARACTERS = Pattern.compile(REGEX);

  private final ICSVParser delegate;

  public CsvRetrofittedVehicleParser(ICSVParser delegate) {
    this.delegate = delegate;
  }

  @Override
  public char getSeparator() {
    return delegate.getSeparator();
  }

  @Override
  public char getQuotechar() {
    return delegate.getQuotechar();
  }

  @Override
  public boolean isPending() {
    return delegate.isPending();
  }

  @Override
  public String[] parseLineMulti(String nextLine) throws IOException {
    checkMaxLineLengthPrecondition(nextLine);
    checkAllowableCharactersPrecondition(nextLine);
    checkTrailingCommaPrecondition(nextLine);

    String[] result = delegate.parseLineMulti(nextLine);

    checkFieldsCountPostcondition(result);
    return result;
  }

  @Override
  public String[] parseLine(String nextLine) throws IOException {
    return delegate.parseLine(nextLine);
  }

  @Override
  public String parseToLine(String[] values, boolean applyQuotesToAll) {
    return delegate.parseToLine(values, applyQuotesToAll);
  }

  @Override
  public CSVReaderNullFieldIndicator nullFieldIndicator() {
    return delegate.nullFieldIndicator();
  }

  @Override
  public String getPendingText() {
    return delegate.getPendingText();
  }

  @Override
  public void setErrorLocale(Locale locale) {
    delegate.setErrorLocale(locale);
  }

  private void checkFieldsCountPostcondition(String[] result) {
    if (result.length < MIN_FIELDS_CNT || result.length > MAX_FIELDS_CNT) {
      throw new CsvInvalidFieldsCountException(
          result.length,
          String.format(LINE_INVALID_FIELDS_CNT_MESSAGE_TEMPLATE, result.length)
      );
    }
  }

  private void checkMaxLineLengthPrecondition(String nextLine) {
    int length = nextLine.length();
    if (length > MAX_LINE_LENGTH) {
      throw new CsvMaxLineLengthExceededException(
          String.format(MAX_LENGTH_MESSAGE_TEMPLATE, length),
          length
      );
    }
  }

  private void checkTrailingCommaPrecondition(String nextLine) {
    String trimmed = nextLine.trim();
    if (trimmed.length() > 0 && lastCharOf(trimmed) == ',') {
      throw new CsvInvalidCharacterParseException();
    }
  }

  private char lastCharOf(String trimmed) {
    return trimmed.charAt(trimmed.length() - 1);
  }

  private void checkAllowableCharactersPrecondition(String nextLine) {
    if (!ALLOWABLE_CHARACTERS.matcher(nextLine).matches()) {
      throw new CsvInvalidCharacterParseException();
    }
  }
}
