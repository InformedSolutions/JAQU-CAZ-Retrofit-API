package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.google.common.base.Strings;
import com.opencsv.ICSVParser;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import java.io.IOException;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.taxiregister.service.exception.CsvMaxLineLengthExceededException;

@ExtendWith(MockitoExtension.class)
class CsvRetrofittedVehicleParserTest {

  @Mock
  private ICSVParser delegate;

  @InjectMocks
  private CsvRetrofittedVehicleParser parser;
  private static final String[] ANY_VALID_OUTPUT = new String[]{"1", "2", "3", "4"};

  @ParameterizedTest
  @ValueSource(strings = {
      "comma ND84VSX,category-3,model-3,2019-04-14",
      "underscore ND84VSX,category-3,model_3,2019-04-14",
      "space ND84VSX,category-3,model 3,2019-04-14",
      "ampersand ND84VSX&,category-3,model-3,2019-04-14",
      "apostrophe ND84V'SX',category-3,model-3,2019-04-14",
      "left parenthesis (ND84VSX,category-3,model-3,2019-04-14(",
      "right parenthesis )OND84VSX,category-3,model-3,2019-04-14(",
      "asterisk N*D84VSX,category-3,model-3,2019-04-14",
      "dot ND84VSX,categor.y-3,mod.el-3,2019-04-14",
      "slash ND84VSX,category-3,model-3,20/19-04-14",
      "percent sign ND84VSX,category-3,model-%3,2019-04-14",
      "exclamation mark !ND84VSX,category-3,model-3,2019-04-14!",
      "plus sign ND84VSX+,category-3,model-3,2019-04-1+4",
      "colon ND84VSX,cat:egory-3,:model-3,2019-04-14",
      "equals sign ND84VSX,category-3,model-3,2019-04=14",
      "question mark ND84VSX,cate?gory-3,mode?l-3,2019-04-14",
      "at sign ND84VSX,cate@gory-3,model-3,2@019-04-14",
      "left square bracket [ND84VSX,category-3,model-3,2019-[04-14",
      "right square bracket ]ND84VSX,category-3,model-3,2019-04]-14",
      "circumflex accent ^ND84VSX,category-3,model-3,^2019-04-14",
      "left curly bracket {ND84VSX,category-3,model-3,2019-04-{14",
      "right curly bracket }ND84VSX,category-3,model-3,2019-04-14}",
      "tilde ~ND84VSX,category-3,model-3,2019-~04-14",
  })
  public void shouldAcceptLinesWithAcceptedCharacters(String line) throws IOException {
    // given
    given(delegate.parseLineMulti(line)).willReturn(ANY_VALID_OUTPUT);

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "pound ND8£4VSX,cat£egory-3,model-3,2019-04-14",
      "dollar N$D84VSX,category-3,model-3,2019-04-14",
      "hash N#D84VSX,category-3,model-3,2019-04#-14",
  })
  public void shouldRejectLinesWithUnacceptedCharacters(String line) {
    // given

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidCharacterParseException.class);
  }

  @Test
  public void shouldRejectEmptyLine() {
    // given
    String line = "";

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidCharacterParseException.class);
  }

  @Test
  public void shouldRejectTooLongLine() {
    // given
    String line = Strings.repeat("a", CsvRetrofittedVehicleParser.MAX_LINE_LENGTH + 1);

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvMaxLineLengthExceededException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "ND84VSX,category-3,model-3,2019-04-14,",
      "ND84VSX,category-3,model-3,2019-04-14,    ",
      "                ,                   ,    ",
      ",    ",
  })
  public void shouldRejectLineContainingTrailingComma(String line) {
    // given

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidCharacterParseException.class);
  }

  @Test
  public void shouldCallDelegateWhenCallingParseLineMulti() throws IOException {
    // given
    String line = "ND84VSX,category-3,model-3,2019-04-14";
    given(delegate.parseLineMulti(line)).willReturn(ANY_VALID_OUTPUT);

    // when
    String[] result = parser.parseLineMulti(line);

    // then
    then(result).containsExactly(ANY_VALID_OUTPUT);
  }

  @Test
  public void shouldCallDelegateWhenCallingGetSeparator() {
    // given
    char separator = ',';
    given(delegate.getSeparator()).willReturn(separator);

    // when
    char result = parser.getSeparator();

    // then
    then(result).isEqualTo(separator);
  }

  @Test
  public void shouldCallDelegateWhenCallingGetQuotechar() {
    // given
    char quoteChar = 'x';
    given(delegate.getQuotechar()).willReturn(quoteChar);

    // when
    char result = parser.getQuotechar();

    // then
    then(result).isEqualTo(quoteChar);
  }

  @Test
  public void shouldCallDelegateWhenCallingIsPending() {
    // given
    boolean isPending = true;
    given(delegate.isPending()).willReturn(isPending);

    // when
    boolean result = parser.isPending();

    // then
    then(result).isEqualTo(isPending);
  }

  @Test
  public void shouldCallDelegateWhenCallingParseLine() throws IOException {
    // given
    String[] output = new String[]{"output"};
    given(delegate.parseLine(any())).willReturn(output);

    // when
    String[] result = parser.parseLine("anything");

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingParseToLine() {
    // given
    String output = "output";
    given(delegate.parseToLine(any(), anyBoolean())).willReturn(output);

    // when
    String result = parser.parseToLine(new String[0], false);

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingNullFieldIndicator() {
    // given
    CSVReaderNullFieldIndicator output = CSVReaderNullFieldIndicator.NEITHER;
    given(delegate.nullFieldIndicator()).willReturn(output);

    // when
    CSVReaderNullFieldIndicator result = parser.nullFieldIndicator();

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingGetPendingText() {
    // given
    String output = "output";
    given(delegate.getPendingText()).willReturn(output);

    // when
    String result = parser.getPendingText();

    // then
    then(result).isEqualTo(output);
  }

  @Test
  public void shouldCallDelegateWhenCallingSetErrorLocale() {
    // given
    Locale locale = Locale.CANADA_FRENCH;

    // when
    parser.setErrorLocale(locale);

    // then
    verify(delegate).setErrorLocale(locale);
  }

  @Test
  public void shouldThrowCsvInvalidFieldsCountExceptionWhenFieldsCntIsTooLow() throws IOException {
    // given
    String line = "any input";
    given(delegate.parseLineMulti(anyString())).willReturn(new String[] {"0"});

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidFieldsCountException.class);
  }

  @Test
  public void shouldThrowCsvInvalidFieldsCountExceptionWhenFieldsCntIsTooHigh() throws IOException {
    // given
    String line = "any input";
    given(delegate.parseLineMulti(anyString()))
        .willReturn(new String[] {"0", "0", "0", "0", "0", "0", "0", "0", "0", "0"});

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidFieldsCountException.class);
  }
}