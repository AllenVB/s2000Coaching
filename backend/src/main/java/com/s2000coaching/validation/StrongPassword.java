package com.s2000coaching.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A password that is long enough and mixes an upper-case letter, a lower-case letter, a digit
 * and a special character. Put only on passwords being <em>chosen</em>: a sign-in request must
 * accept whatever the account already has.
 *
 * <p>The upper limit is 64 because bcrypt only reads the first 72 bytes — a longer password
 * would be accepted and silently cut short.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

	int min() default 8;

	int max() default 64;

	String message() default "Şifre en az {min} karakter olmalı ve en az bir büyük harf, bir küçük harf, "
			+ "bir rakam ve bir özel karakter içermeli.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
