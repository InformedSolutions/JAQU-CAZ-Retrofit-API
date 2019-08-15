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
import uk.gov.caz.taxiregister.service.exception.CsvInvalidBooleanValueException;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidCharacterParseException;
import uk.gov.caz.taxiregister.service.exception.CsvInvalidFieldsCountException;
import uk.gov.caz.taxiregister.service.exception.CsvMaxLineLengthExceededException;

@ExtendWith(MockitoExtension.class)
class CsvLicenceParserTest {

  @Mock
  private ICSVParser delegate;

  @InjectMocks
  private CsvLicenceParser parser;
  private static final String[] ANY_VALID_OUTPUT = new String[]{"1", "2", "3", "4", "5", "6"};

  @ParameterizedTest
  @ValueSource(strings = {
      "comma OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true",
      "underscore OI64E_FO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true",
      "space O I 6 4EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true",
      "ampersand OI64EFO&,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true",
      "apostrophe 'O'I64EFO&,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true",
      "left parenthesis (OI64EFO&,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true(",
      "right parenthesis )OI64EFO&,2019-04-30),2019-05-22,taxi,la-3,dJfRR,true(",
      "asterisk *OI64*(-EFO&,2019-04-30),2019-05-22,taxi,la-3,dJfRR,true(",
      "dot .OI64EFO&,2019-04-30),2019-05-2.2,taxi,la-3,dJfRR,true",
      "slash /OI64EFO&,2019-04-30,2/019-05-2.2,taxi,la-3,dJfRR,true",
      "percent sign %OI64EFO&,2019-04-30,2/019-05-2.2,taxi,la-3,dJfRR,true%",
      "exclamation mark !OI64E!FO,2019-04-30,2/019-05-2.2,taxi,la-3,dJfRR,t!rue",
      "plus sign +OI64EFO,201+9-04-30,2/019-05-2.2,taxi,la-3,dJfRR,t!rue",
      "colon :OI64EF:O,201+9-04-30,2/019-05-2.2,taxi,la-3,dJfRR:,true",
      "equals sign O=I64EFO,2019;-04-30,2/019-05-2=2,taxi,la-3,dJfRR:,true",
      "question mark O?I64EFO,201904-30,2/019-05-2=2,tax?i,la-3,dJfRR:,true",
      "at sign O@I64EFO,201904-30,@2019-05-2=2,taxi,la-3,dJfRR,true",
      "left square bracket [OI64EFO,201904-30,2019-05-22,taxi,la-3,dJfRR,[true",
      "right square bracket ]OI64EFO,201904-30,2019-05-22,taxi,la-3,dJfRR,true]",
      "circumflex accent ^OI64EFO,^2019-04-30,2019-05-22,taxi,la-3,dJfRR,true",
      "left curly bracket {OI64EFO,2019-04-30,2019-05-22,taxi,{la-3,dJfRR,true",
      "right curly bracket }OI64EFO,2019-04-30,2019-05-22,taxi,}la-3,dJfRR,true",
      "tilde ~OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,tr~ue",
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
      "pound £OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true",
      "dollar $OI64EFO,2019-04-30,2019-05-22,taxi,l$a-3,dJfRR,true",
      "hash #OI64EFO,2019-04-30,2019-05-22,taxi,la-3,#dJfRR,true",
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
    String line = Strings.repeat("a", CsvLicenceParser.MAX_LINE_LENGTH + 1);

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvMaxLineLengthExceededException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true,",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true,    ",
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
    String line = "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,true";
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

  @ParameterizedTest
  @ValueSource(strings = {
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,t",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,f",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,0",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,1",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,True",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,False",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,TrUe",
      "OI64EFO,2019-04-30,2019-05-22,taxi,la-3,dJfRR,FaLsE",
  })
  public void shouldRejectLinesWithInvalidBooleanValuesForWheelchairAccessibleFlag(String line)
      throws IOException {
    // given
    String[] result = line.split(",");
    given(delegate.parseLineMulti(line)).willReturn(result);

    // when
    Throwable throwable = catchThrowable(() -> parser.parseLineMulti(line));

    // then
    then(throwable).isInstanceOf(CsvInvalidBooleanValueException.class);
  }
}