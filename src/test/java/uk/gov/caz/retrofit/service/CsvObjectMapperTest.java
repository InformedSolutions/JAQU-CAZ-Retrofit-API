package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.BDDAssertions.then;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.CsvParseResult;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.service.validation.CsvAwareValidationMessageModifier;

class CsvObjectMapperTest {

  private CsvObjectMapper csvObjectMapper = new CsvObjectMapper(
      new CsvAwareValidationMessageModifier());

  @Test
  public void shouldReadValidCsvData() throws IOException {
    // given
    String csvLine = "ZC62OMB,category-2,model-2,2019-05-17";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getRetrofittedVehicles()).containsOnly(
        RetrofittedVehicleDto.builder()
            .vrn("ZC62OMB")
            .vehicleCategory("category-2")
            .model("model-2")
            .dateOfRetrofitInstallation("2019-05-17")
            .lineNumber(1)
            .build()
    );
  }

  @Test
  public void shouldIgnoreLinesWithExtraValues() throws IOException {
    // given
    String csvLine = "ZC62OMB,category-2,model-2,2019-05-17,extraValue1";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getRetrofittedVehicles()).isEmpty();
  }

  @Test
  public void shouldIgnoreTooLongLines() throws IOException {
    // given
    String csvLine = "ZC62OMB,category-2,model-2,2019-05-17" + Strings.repeat("ab", 100);

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getRetrofittedVehicles()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"\n", " ", "\t"})
  public void shouldIgnoreLinesWithWhitespaces(String csvLine) throws IOException {
    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getRetrofittedVehicles()).isEmpty();
    then(result.getValidationErrors()).hasSize(1);
  }

  @Test
  public void shouldIgnoreLinesWithUnacceptedCharacters() throws IOException {
    // given
    // contains '$' and '#'
    String csvLine = "Z$C62OMB,c#ategory-2,model-2,2019-05-17\n"
        + "ND84VSX,category-3,model-3,2019-04-14";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getRetrofittedVehicles()).containsExactly(
        RetrofittedVehicleDto.builder()
            .vrn("ND84VSX")
            .vehicleCategory("category-3")
            .model("model-3")
            .dateOfRetrofitInstallation("2019-04-14")
            .lineNumber(2)
            .build()
    );
    then(result.getValidationErrors()).containsExactly(
        ValidationError.valueError("Line contains invalid character(s), is empty or has "
            + "trailing comma character. Please make sure you have not included a header row.", 1)
    );
  }

  @Test
  public void shouldIncludeInformationAboutHeaderAndTrailingRow() throws IOException {
    // given
    String csvLine = "VRN,category,model,dateWithNotAllowedCharacter$\n"
        + ",,,,,,";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    then(result.getRetrofittedVehicles()).isEmpty();
    then(result.getValidationErrors()).containsExactly(
        ValidationError.valueError(
            "Line contains invalid character(s), is empty or has trailing comma character. Please make sure you have not included a header row.",
            1),
        ValidationError.valueError(
            "Line contains invalid character(s), is empty or has trailing comma character. Please make sure you have not included a trailing row.",
            2)
    );
  }

  @Test
  public void shouldIgnoreLinesWithTooFewAttributes() throws IOException {
    // given
    String csvLine = "ZC62OMB,category-2,model-2,2019-05-17\n"
        + "DL76MWX,2019-04-11\n"
        + "ND84VSX,category-3,model-3,2019-04-14";

    // when
    CsvParseResult result = csvObjectMapper.read(toInputStream(csvLine));

    // then
    RetrofittedVehicleDto[] expected = Arrays.array(
        RetrofittedVehicleDto.builder()
            .vrn("ZC62OMB")
            .vehicleCategory("category-2")
            .model("model-2")
            .dateOfRetrofitInstallation("2019-05-17")
            .lineNumber(1)
            .build(),
        RetrofittedVehicleDto.builder()
            .vrn("ND84VSX")
            .vehicleCategory("category-3")
            .model("model-3")
            .dateOfRetrofitInstallation("2019-04-14")
            .lineNumber(3)
            .build()
    );
    then(result.getRetrofittedVehicles()).containsOnly(expected);
    then(result.getValidationErrors()).hasOnlyOneElementSatisfying(
        validationError -> then(validationError.getDetail())
            .startsWith("Line 2: Line contains invalid number of fields")
    );
  }

  private ByteArrayInputStream toInputStream(String csvLine) {
    return new ByteArrayInputStream(csvLine.getBytes(Charsets.UTF_8));
  }
}