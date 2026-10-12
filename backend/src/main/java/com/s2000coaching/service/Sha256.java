package com.s2000coaching.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Hex SHA-256 used to persist verification codes and session tokens as digests only. */
final class Sha256 {

	private Sha256() {
	}

	static String hex(String value) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 bulunamadı", ex);
		}
		byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		return sb.toString();
	}

	/** Constant-time comparison of two hex digests. */
	static boolean matches(String expectedHex, String actualHex) {
		return MessageDigest.isEqual(
				expectedHex.getBytes(StandardCharsets.UTF_8),
				actualHex.getBytes(StandardCharsets.UTF_8));
	}
}
