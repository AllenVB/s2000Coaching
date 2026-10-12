package com.s2000coaching.storage;

/** Storage itself failed — a full disk, a permission error, an unreachable bucket. */
public class PhotoStorageException extends RuntimeException {

	public PhotoStorageException(String message) {
		super(message);
	}

	public PhotoStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
