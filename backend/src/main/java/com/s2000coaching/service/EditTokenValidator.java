package com.s2000coaching.service;

import com.s2000coaching.exception.ForbiddenException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Gate for the shared passphrase that protects every public-content write in this app
 * (currently just "Tecrübelerim"). There is no user/session auth system, so this is the
 * whole access-control story for those endpoints.
 */
@Component
public class EditTokenValidator {

	private static final Logger log = LoggerFactory.getLogger(EditTokenValidator.class);
	private static final String INSECURE_DEFAULT_TOKEN = "s2000-dev-edit";

	private final String editToken;

	public EditTokenValidator(@Value("${app.experience.edit-token}") String editToken) {
		this.editToken = editToken;
		if (INSECURE_DEFAULT_TOKEN.equals(editToken)) {
			log.warn("app.experience.edit-token is using the insecure local-dev default. "
					+ "Set EXPERIENCE_EDIT_TOKEN before deploying anywhere reachable by the public.");
		}
	}

	public void requireValid(String providedToken) {
		if (!isValid(providedToken)) {
			throw new ForbiddenException("Düzenleme parolası geçersiz");
		}
	}

	private boolean isValid(String providedToken) {
		if (providedToken == null) {
			return false;
		}
		byte[] expected = editToken.getBytes(StandardCharsets.UTF_8);
		byte[] actual = providedToken.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
	}
}
