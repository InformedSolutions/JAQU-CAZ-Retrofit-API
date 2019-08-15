package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.BDDAssertions.then;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.CsvParseResult;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.service.validation.CsvAwareValidationMessageModifier;

class CsvObjectMapperTest {

  private CsvObjectMapper csvObjectMapper = new CsvObjectMapper(
      new CsvAwareValidationMessageModifier());

  @Test
  public void shouldReadValidCsvData() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,true";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).containsOnly(
        VehicleDto.builder()
            .vrm("ZC62OMB")
            .start("2019-04-15")
            .end("2019-05-17")
            .taxiOrPhv("PHV")
            .licensingAuthorityName("InmxgozMZS")
            .licensePlateNumber("beBCC")
            .wheelchairAccessibleVehicle(true)
            .lineNumber(1)
            .build()
    );
  }

  @Test
  public void shouldIgnoreLinesWithExtraValues() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,false,extraValue1,extraValue2";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).isEmpty();
  }

  @Test
  public void shouldIgnoreTooLongLines() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS" + Strings.repeat("ab", 100);

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"\n", " ", "\t"})
  public void shouldIgnoreLinesWithWhitespaces(String csvLine) throws IOException {
    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).isEmpty();
    then(result.getValidationErrors()).hasSize(1);
  }

  @Test
  public void shouldIgnoreLinesWithUnacceptedCharacters() throws IOException {
    // given
    // contains '$' and '#'
    String csvLine = "$ZC62OMB,#2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,false\n"
        + "ND84VSX,2019-04-14,2019-06-13,taxi,FBVoeKJGZF,Oretr,true";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).containsExactly(
        VehicleDto.builder()
            .vrm("ND84VSX")
            .start("2019-04-14")
            .end("2019-06-13")
            .taxiOrPhv("taxi")
            .licensingAuthorityName("FBVoeKJGZF")
            .licensePlateNumber("Oretr")
            .wheelchairAccessibleVehicle(true)
            .lineNumber(2)
            .build()
    );
    then(result.getValidationErrors()).containsExactly(
        ValidationError.valueError("Line contains invalid character(s), is empty or has "
            + "trailing comma character. Please make sure you have not included a header row.", 1)
    );
  }

  @Test
  public void shouldIgnoreLineWithWrongBooleanFlag() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,1";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).isEmpty();
    then(result.getValidationErrors()).containsExactly(
        ValidationError.valueError("Line contains invalid boolean value. "
            + "Please make sure you have not included a header row. "
            + "Please make sure you have not included a trailing row.", 1)
    );
  }

  @Test
  public void shouldIncludeInformationAboutHeaderAndTrailingRow() throws IOException {
    // given
    String csvLine = "VRM,start,end,type,plateNo,sth,wheelchairAccessible\n"
        + ",,,,,,";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).isEmpty();
    then(result.getValidationErrors()).containsExactly(
        ValidationError.valueError(
            "Line contains invalid boolean value. Please make sure you have not included a header row.",
            1),
        ValidationError.valueError(
            "Line contains invalid character(s), is empty or has trailing comma character. Please make sure you have not included a trailing row.",
            2)
    );
  }

  @Test
  public void shouldAcceptOptionalWheelchairValue() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getLicences()).containsOnly(
        VehicleDto.builder()
            .vrm("ZC62OMB")
            .start("2019-04-15")
            .end("2019-05-17")
            .taxiOrPhv("PHV")
            .licensingAuthorityName("InmxgozMZS")
            .licensePlateNumber("beBCC")
            .wheelchairAccessibleVehicle(null)
            .lineNumber(1)
            .build()
    );
  }

  @Test
  public void shouldIgnoreLinesWithTooFewAttributes() throws IOException {
    // given
    String csvLine = "ZC62OMB,2019-04-15,2019-05-17,PHV,InmxgozMZS,beBCC,false\n"
        + "DL76MWX,2019-04-11\n"
        + "ND84VSX,2019-04-14,2019-06-13,taxi,FBVoeKJGZF,Oretr,true";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    VehicleDto[] expected = Arrays.array(
        VehicleDto.builder()
            .vrm("ZC62OMB")
            .start("2019-04-15")
            .end("2019-05-17")
            .taxiOrPhv("PHV")
            .licensingAuthorityName("InmxgozMZS")
            .licensePlateNumber("beBCC")
            .wheelchairAccessibleVehicle(false)
            .lineNumber(1)
            .build(),
        VehicleDto.builder()
            .vrm("ND84VSX")
            .start("2019-04-14")
            .end("2019-06-13")
            .taxiOrPhv("taxi")
            .licensingAuthorityName("FBVoeKJGZF")
            .licensePlateNumber("Oretr")
            .wheelchairAccessibleVehicle(true)
            .lineNumber(3)
            .build()
    );
    then(result.getLicences()).containsOnly(expected);
    then(result.getValidationErrors()).hasOnlyOneElementSatisfying(
        validationError -> then(validationError.getDetail())
            .startsWith("Line 2: Line contains invalid number of fields")
    );
  }

  private ByteArrayInputStream toInputStream(String csvLine) {
    return new ByteArrayInputStream(csvLine.getBytes(Charsets.UTF_8));
  }
}