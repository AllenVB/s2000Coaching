package com.s2000coaching.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Photos on the local disk, under a configured root.
 *
 * <p>Keys are generated here and are opaque to callers: a random name under a two-level fan
 * out, so a few thousand photos do not land in one directory. Nothing from the upload — not
 * the member, not the original filename — goes into the key, so a key leaking tells an
 * attacker nothing and a hostile filename cannot escape the root.
 *
 * <p>This is the single-server option. A deployment on more than one machine needs shared
 * storage or an object-storage implementation of {@link PhotoStorage}.
 */
@Service
public class FilesystemPhotoStorage implements PhotoStorage {

	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/jpeg", ".jpg",
			"image/png", ".png",
			"image/webp", ".webp");

	private final Path root;

	public FilesystemPhotoStorage(@Value("${app.photos.storage-path}") String storagePath) {
		this.root = Path.of(storagePath).toAbsolutePath().normalize();
	}

	@Override
	public String store(byte[] content, String contentType) {
		String key = generateKey(contentType);
		Path target = resolve(key);
		try {
			Files.createDirectories(target.getParent());
			// CREATE_NEW rather than an overwrite: a generated key colliding with an existing
			// file means the randomness is broken, and silently replacing someone else's photo
			// is the worst possible response to that.
			Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		} catch (IOException e) {
			throw new PhotoStorageException("Fotoğraf kaydedilemedi.", e);
		}
		return key;
	}

	@Override
	public InputStream read(String storageKey) {
		Path source = resolve(storageKey);
		try {
			return Files.newInputStream(source);
		} catch (IOException e) {
			throw new StoredFileNotFoundException("Fotoğrafın dosyası bulunamadı: " + storageKey, e);
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			Files.deleteIfExists(resolve(storageKey));
		} catch (IOException e) {
			throw new PhotoStorageException("Fotoğraf silinemedi.", e);
		}
	}

	private String generateKey(String contentType) {
		String id = UUID.randomUUID().toString();
		String extension = EXTENSIONS.getOrDefault(
				contentType == null ? "" : contentType.toLowerCase(Locale.ROOT), "");
		return "%s/%s/%s%s".formatted(id.substring(0, 2), id.substring(2, 4), id, extension);
	}

	/**
	 * Resolves a key under the root and refuses anything that climbs out of it. Keys are
	 * generated here, so this should never fire — but it is the one check standing between a
	 * corrupted or hand-edited key and the rest of the filesystem.
	 */
	private Path resolve(String storageKey) {
		Path resolved = root.resolve(storageKey).normalize();
		if (!resolved.startsWith(root)) {
			throw new PhotoStorageException("Geçersiz dosya anahtarı: " + storageKey);
		}
		return resolved;
	}
}
