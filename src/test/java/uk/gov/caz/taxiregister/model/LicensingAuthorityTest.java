package uk.gov.caz.taxiregister.model;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LicensingAuthorityTest {
  @Test
  public void shouldThrowNullPointerExceptionIfNameIsNullAndIdIsNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new LicensingAuthority(null, null));

    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> LicensingAuthority.withNameOnly(null));
  }

  @Test
  public void shouldThrowNullPointerExceptionIfNameIsNullAndIdIsNotNull() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new LicensingAuthority(1, null));
  }

  @Test
  public void shouldContainPassedValidValuesWithNullId() {
    String name = "validName";
    Integer id = null;

    LicensingAuthority licensingAuthority = new LicensingAuthority(id, name);

    assertThat(licensingAuthority.getName()).isEqualTo(name);
    assertThat(licensingAuthority.getId()).isNull();
  }

  @Test
  public void shouldContainPassedValidValuesWithNullIdWhenCreatedByFactoryMethod() {
    String value = "validName";

    LicensingAuthority licensingAuthority = LicensingAuthority.withNameOnly(value);

    assertThat(licensingAuthority.getName()).isEqualTo(value);
    assertThat(licensingAuthority.getId()).isNull();
  }

  @Test
  public void shouldContainPassedValidValuesWithNonNullId() {
    String name = "validName";
    Integer id = 15;

    LicensingAuthority licensingAuthority = new LicensingAuthority(id, name);

    assertThat(licensingAuthority.getName()).isEqualTo(name);
    assertThat(licensingAuthority.getId()).isEqualTo(id);
  }
}