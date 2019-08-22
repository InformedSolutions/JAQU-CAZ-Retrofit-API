package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.BDDAssertions.then;

import com.opencsv.CSVParser;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class CsvRetrofittedVehicleParserTestIT {

  private CsvRetrofittedVehicleParser csvRetrofittedVehicleParser = new CsvRetrofittedVehicleParser(new CSVParser());

  @Test
  public void shouldParseLineWithComma() throws IOException {
    // given
    String line = "ZC62OMB,\"Field with a comma, comma\",model-2,2019-05-17";

    // when
    String[] result = csvRetrofittedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB", "Field with a comma, comma", "model-2", "2019-05-17"});
  }

  @Test
  public void shouldParseLineWithDoubleQuote() throws IOException {
    // given
    String line = "ZC62OMB,\"\"Hooyah!\"\",model-2,2019-05-17";

    // when
    String[] result = csvRetrofittedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB", "\"Hooyah!\"", "model-2", "2019-05-17"});
  }

  @Test
  public void shouldParseLineWithCommaAndDoubleQuote() throws IOException {
    // given
    String line = "\"\"\"\"\"\"," // two quotes: ""
        + "\",,\"," // two commas: ,,
        + "\"\"\",\"\",\"\"\"," // three quotes separated by commas: ",","
        + "\"\"\",,\"\"\""; // quote, comma, comma, quote

    // when
    String[] result = csvRetrofittedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{
        "\"\"",
        ",,",
        "\",\",\"",
        "\",,\""
    });
  }

  @Test
  public void shouldParseLineWithAmpersandAndApostrophe() throws IOException {
    // given
    String line = "ZC62OMB,category-2,a & b'c & d,2019-05-17";

    // when
    String[] result = csvRetrofittedVehicleParser.parseLineMulti(line);

    // then
    then(result).isEqualTo(new String[]{"ZC62OMB", "category-2", "a & b'c & d", "2019-05-17"});
  }
}