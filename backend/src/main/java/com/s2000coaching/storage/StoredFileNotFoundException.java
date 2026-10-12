package com.s2000coaching.storage;

/**
 * The row survived but its bytes did not — a half-restored backup, a wiped volume, a key
 * deleted by hand. Separate from {@code NotFoundException} so the caller can tell "no such
 * photo" from "the photo's file is missing", which need different fixes.
 */
public class StoredFileNotFoundException extends RuntimeException {

	public StoredFileNotFoundException(String message) {
		super(message);
	}

	public StoredFileNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
