package uk.gov.caz.retrofit.service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.CsvParseResult;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.service.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.retrofit.service.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.retrofit.service.exception.CsvMaxLineLengthExceededException;
import uk.gov.caz.retrofit.service.validation.CsvAwareValidationMessageModifier;

/**
 * A class that provides methods to map an {@link InputStream} of a CSV data to a {@link Set} of
 * {@link RetrofittedVehicle}.
 */
@Component
@Slf4j
public class CsvObjectMapper {

  private static final String LINE_TOO_LONG_MESSAGE_TEMPLATE = "Line is too long (actual value: "
      + "%d, allowed value: %d).";
  private static final String LINE_INVALID_FORMAT_MESSAGE = "Line contains invalid "
      + "character(s), is empty or has trailing comma character.";
  private static final String LINE_INVALID_FIELDS_COUNT_MESSAGE_TEMPLATE = "Line contains "
      + "invalid number of fields (actual value: %d, allowable value: %d).";

  private final CsvAwareValidationMessageModifier messageModifier;
  private final int maxErrorsCount;

  public CsvObjectMapper(CsvAwareValidationMessageModifier messageModifier,
      @Value("${application.validation.max-errors-count}") int maxErrorsCount) {
    this.messageModifier = messageModifier;
    this.maxErrorsCount = maxErrorsCount;
  }

  /**
   * Reads data from {@code inputStream} and maps it to a {@link CsvParseResult}. The {@code
   * inputStream} *MUST* contain data in CSV format. The {@code inputStream} *MUST* be closed by the
   * client code to avoid memory leaks.
   *
   * @param inputStream A stream which contains data in CSV format
   * @return {@link CsvParseResult}
   */
  public CsvParseResult read(InputStream inputStream) throws IOException {
    ImmutableList.Builder<RetrofittedVehicleDto> vehiclesBuilder = ImmutableList.builder();
    LinkedList<ValidationError> errors = Lists.newLinkedList();
    CSVReader reader = createReader(inputStream);

    String[] fields;
    int lineNo = 1;
    while (errors.size() < maxErrorsCount && (fields = readLine(reader, errors, lineNo)) != null) {
      if (fields.length == 0) {
        log.trace("Validation error on line {}, skipping it", lineNo);
      } else {
        RetrofittedVehicleDto retrofittedVehicleDto = createRetrofittedVehicle(fields, lineNo);
        vehiclesBuilder.add(retrofittedVehicleDto);
        log.debug("Retrofitted vehicle read: {}", retrofittedVehicleDto);
      }
      lineNo += 1;
    }
    logParsingEndReason(errors);
    addTrailingRowErrorInfoIfApplicable(reader, errors, lineNo - 1);

    return new CsvParseResult(vehiclesBuilder.build(), Collections.unmodifiableList(errors));
  }

  private void logParsingEndReason(LinkedList<ValidationError> errors) {
    if (errors.size() >= maxErrorsCount) {
      log.info("Finished parsing the input file: error max count ({}) reached", maxErrorsCount);
    } else {
      log.info("Finished parsing the input file: reached EOF");
    }
  }

  private void addTrailingRowErrorInfoIfApplicable(CSVReader reader,
      LinkedList<ValidationError> errors, int numberOfLines) throws IOException {
    if (hasNotParsedWholeFile(reader) || errors.isEmpty()) {
      log.trace("Skipped adding the info about the trailing row (errors size: {})", errors.size());
      return;
    }
    ValidationError lastError = errors.pollLast(); // lastError != null
    ValidationError lastErrorReplacement = computeLastErrorReplacement(numberOfLines, lastError);
    errors.add(lastErrorReplacement);
  }

  private boolean hasNotParsedWholeFile(CSVReader reader) throws IOException {
    return reader.peek() != null;
  }

  private ValidationError computeLastErrorReplacement(int numberOfLines,
      ValidationError lastError) {
    if (validationErrorHappenedOnLastLine(numberOfLines, lastError)) {
      String newDetail = messageModifier.addTrailingRowInfoSuffix(lastError.getRawDetail());
      return ValidationError.copyWithNewDetail(lastError, newDetail);
    }
    return lastError;
  }

  private boolean validationErrorHappenedOnLastLine(int numberOfLines, ValidationError lastError) {
    // assertion: lastError != null
    return lastError.getLineNumber().map(lineNo -> lineNo == numberOfLines).orElse(Boolean.FALSE);
  }

  private CSVReader createReader(InputStream inputStream) {
    CSVReaderBuilder csvReaderBuilder = new CSVReaderBuilder(new InputStreamReader(inputStream));
    csvReaderBuilder.withCSVParser(new CsvRetrofittedVehicleParser(new CSVParser()));
    return csvReaderBuilder.build();
  }

  private RetrofittedVehicleDto createRetrofittedVehicle(String[] fields, int lineNo) {
    return RetrofittedVehicleDto.builder()
        .vrn(StringUtils.deleteWhitespace(fields[0]))
        .vehicleCategory(Strings.emptyToNull(fields[1]))
        .model(Strings.emptyToNull(fields[2]))
        .dateOfRetrofitInstallation(fields[3])
        .lineNumber(lineNo)
        .build();
  }

  private ValidationError createInvalidFieldsCountError(int lineNo,
      CsvInvalidFieldsCountException e) {
    return ValidationError.valueError(
        modifyErrorMessage(lineNo, invalidFieldsCountErrorDetail(e)), lineNo);
  }

  private ValidationError createMaximumLineLengthExceededError(int lineNo,
      CsvMaxLineLengthExceededException e) {
    return ValidationError.valueError(
        modifyErrorMessage(lineNo, maximumLineLengthErrorDetail(e)), lineNo);
  }

  private ValidationError createParseValidationError(int lineNo) {
    return ValidationError.valueError(
        modifyErrorMessage(lineNo, LINE_INVALID_FORMAT_MESSAGE), lineNo);
  }

  private String invalidFieldsCountErrorDetail(CsvInvalidFieldsCountException e) {
    return String.format(LINE_INVALID_FIELDS_COUNT_MESSAGE_TEMPLATE, e.getFieldsCount(),
        CsvRetrofittedVehicleParser.EXPECTED_FIELDS_CNT);
  }

  private String maximumLineLengthErrorDetail(CsvMaxLineLengthExceededException e) {
    return String.format(LINE_TOO_LONG_MESSAGE_TEMPLATE, e.getLineLength(),
        CsvRetrofittedVehicleParser.MAX_LINE_LENGTH);
  }

  /**
   * Reads line and returns null if end of the stream, empty array if validation error, non-empty
   * array on success.
   */
  private String[] readLine(CSVReader reader, LinkedList<ValidationError> errors, int lineNo)
      throws IOException {
    try {
      return reader.readNext();
    } catch (CsvInvalidFieldsCountException e) {
      log.debug("Invalid number of fields detected: {}", e.getMessage());
      errors.add(createInvalidFieldsCountError(lineNo, e));
    } catch (CsvMaxLineLengthExceededException e) {
      log.debug("Maximum line length exceeded: {}", e.getMessage());
      errors.add(createMaximumLineLengthExceededError(lineNo, e));
    } catch (CsvInvalidCharacterParseException e) {
      log.debug("Error while parsing line {}: {}", lineNo, e.getMessage());
      errors.add(createParseValidationError(lineNo));
    }
    return new String[0];
  }

  private String modifyErrorMessage(int lineNumber, String message) {
    return messageModifier.addHeaderRowInfoSuffix(message, lineNumber);
  }
}
