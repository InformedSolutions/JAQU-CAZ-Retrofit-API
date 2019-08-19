package uk.gov.caz.taxiregister.service;

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
import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;
import uk.gov.caz.taxiregister.model.CsvParseResult;
import uk.gov.caz.taxiregister.model.RetrofittedVehicle;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.taxiregister.service.exception.CsvMaxLineLengthExceededException;
import uk.gov.caz.taxiregister.service.validation.CsvAwareValidationMessageModifier;

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

  public CsvObjectMapper(CsvAwareValidationMessageModifier messageModifier) {
    this.messageModifier = messageModifier;
  }

  /**
   * Reads data from {@code inputStream} and maps it to a {@link CsvParseResult}. The {@code
   * inputStream} *MUST* contain data in CSV format. The {@code inputStream} *MUST* be closed by the
   * client code to avoid memory leaks.
   *
   * @param inputStream A stream which contains data in CSV format
   * @return {@link CsvParseResult}
   */
  CsvParseResult read(InputStream inputStream) throws IOException {
    ImmutableList.Builder<RetrofittedVehicleDto> vehiclesBuilder = ImmutableList.builder();
    LinkedList<ValidationError> errors = Lists.newLinkedList();
    CSVReader csvReader = createReader(inputStream);

    String[] fields;
    int lineNo;
    for (lineNo = 1; (fields = readLine(csvReader, errors, lineNo)) != null; ++lineNo) {
      if (fields.length == 0) {
        log.trace("Validation error on line {}, skipping it", lineNo);
      } else {
        RetrofittedVehicleDto retrofittedVehicleDto = createRetrofittedVehicle(fields, lineNo);
        vehiclesBuilder.add(retrofittedVehicleDto);
        log.debug("Retrofitted vehicle read: {}", retrofittedVehicleDto);
      }
    }
    addTrailingRowErrorInfoIfApplicable(errors, lineNo - 1);

    return new CsvParseResult(vehiclesBuilder.build(), Collections.unmodifiableList(errors));
  }

  private void addTrailingRowErrorInfoIfApplicable(LinkedList<ValidationError> errors,
      int numberOfLines) {
    if (errors.isEmpty()) {
      return;
    }
    ValidationError lastError = errors.pollLast(); // lastError != null
    ValidationError lastErrorReplacement = computeLastErrorReplacement(numberOfLines, lastError);
    errors.add(lastErrorReplacement);
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
        .vrn(fields[0])
        .vehicleCategory(fields[1])
        .model(fields[2])
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
