package com.s2000coaching.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StrongPasswordValidatorTest {

	private final StrongPasswordValidator validator = new StrongPasswordValidator();

	@ParameterizedTest
	@ValueSource(strings = { "Gizli-parola1", "1234567Ab.", "Aa1!aaaa", "ŞifreÇok1*", "Ab1 çözüm!" })
	void shouldAcceptAPasswordWithEveryKindOfCharacter(String password) {
		assertThat(StrongPasswordValidator.isStrong(password, 8, 64)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Aa1!aaa",          // seven characters
			"gizli-parola1",    // no upper case
			"GIZLI-PAROLA1",    // no lower case
			"Gizli-parola",     // no digit
			"Gizliparola12",    // no special character
			"        ",         // spaces are not a special character
	})
	void shouldRejectAPasswordMissingAKindOrOutOfLength(String password) {
		assertThat(StrongPasswordValidator.isStrong(password, 8, 64)).isFalse();
	}

	@org.junit.jupiter.api.Test
	void shouldRejectAPasswordOverTheLimit() {
		assertThat(StrongPasswordValidator.isStrong("Aa1!" + "a".repeat(61), 8, 64)).isFalse();
		assertThat(StrongPasswordValidator.isStrong("Aa1!" + "a".repeat(60), 8, 64)).isTrue();
	}

	@org.junit.jupiter.api.Test
	void shouldLeaveNullToNotBlank() {
		assertThat(validator.isValid(null, null)).isTrue();
	}
}
