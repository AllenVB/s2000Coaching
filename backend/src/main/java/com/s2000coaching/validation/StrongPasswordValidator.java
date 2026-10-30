package com.s2000coaching.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

	private int min;
	private int max;

	@Override
	public void initialize(StrongPassword annotation) {
		this.min = annotation.min();
		this.max = annotation.max();
	}

	/** Null is left to {@code @NotBlank}, which every password field also carries. */
	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		return password == null || isStrong(password, min, max);
	}

	public static boolean isStrong(String password, int min, int max) {
		if (password.length() < min || password.length() > max) {
			return false;
		}
		boolean upper = false;
		boolean lower = false;
		boolean digit = false;
		boolean special = false;
		for (int i = 0; i < password.length(); ) {
			int c = password.codePointAt(i);
			i += Character.charCount(c);
			if (Character.isUpperCase(c)) {
				upper = true;
			} else if (Character.isLowerCase(c)) {
				lower = true;
			} else if (Character.isDigit(c)) {
				digit = true;
			} else if (!Character.isLetter(c) && !Character.isWhitespace(c)) {
				special = true;
			}
		}
		return upper && lower && digit && special;
	}
}
