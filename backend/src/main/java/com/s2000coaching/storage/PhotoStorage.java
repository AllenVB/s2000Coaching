package com.s2000coaching.storage;

import java.io.InputStream;

/**
 * Seam for where progress photos live. {@link FilesystemPhotoStorage} is the only
 * implementation today; object storage (S3 and the like) can be added later as another
 * {@code @Service} bean without touching the service or the controllers.
 *
 * <p>The database stores only the key this returns, never the bytes — photos are large and
 * would bloat every backup and every query plan that touches the table.
 */
public interface PhotoStorage {

	/**
	 * Writes the bytes and answers the key they can be read back by. Implementations choose
	 * the key, so nothing above this interface may assume it is a path.
	 */
	String store(byte[] content, String contentType);

	/** @throws StoredFileNotFoundException when the key has no bytes behind it. */
	InputStream read(String storageKey);

	/** Deleting a key that is already gone succeeds: the caller wanted it absent, and it is. */
	void delete(String storageKey);
}
