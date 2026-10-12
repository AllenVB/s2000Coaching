package com.s2000coaching.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FilesystemPhotoStorageTest {

	private static final byte[] CONTENT = "fotoğraf baytları".getBytes();

	@TempDir
	Path root;

	private FilesystemPhotoStorage storage;

	@BeforeEach
	void setUp() {
		storage = new FilesystemPhotoStorage(root.toString());
	}

	@Test
	void shouldStoreAndReadBack() throws IOException {
		String key = storage.store(CONTENT, "image/jpeg");

		try (InputStream in = storage.read(key)) {
			assertThat(in.readAllBytes()).isEqualTo(CONTENT);
		}
		assertThat(root.resolve(key)).exists();
	}

	@ParameterizedTest
	@CsvSource({ "image/jpeg,.jpg", "image/png,.png", "image/webp,.webp" })
	void shouldEndTheKeyWithTheMatchingExtension(String contentType, String extension) {
		assertThat(storage.store(CONTENT, contentType)).endsWith(extension);
	}

	/**
	 * Nothing from the upload goes into the key: a leaked key must say nothing about who the
	 * photo belongs to, and a hostile filename must not be able to steer where it lands.
	 */
	@Test
	void shouldGenerateADistinctFannedOutKeyEveryTime() {
		String first = storage.store(CONTENT, "image/jpeg");
		String second = storage.store(CONTENT, "image/jpeg");

		assertThat(first).isNotEqualTo(second).matches("[0-9a-f]{2}/[0-9a-f]{2}/[0-9a-f-]{36}\\.jpg");
	}

	@Test
	void shouldDeleteAKeyAndTolerateDeletingItTwice() {
		String key = storage.store(CONTENT, "image/jpeg");

		storage.delete(key);

		assertThat(root.resolve(key)).doesNotExist();
		// The caller wanted it absent, and it is.
		assertThatCode(() -> storage.delete(key)).doesNotThrowAnyException();
	}

	@Test
	void shouldFailLoudlyWhenTheFileIsGone() {
		String key = storage.store(CONTENT, "image/jpeg");
		storage.delete(key);

		assertThatThrownBy(() -> storage.read(key)).isInstanceOf(StoredFileNotFoundException.class);
	}

	/**
	 * Keys are generated here, so this should never fire — but it is the one check standing
	 * between a corrupted or hand-edited key and the rest of the filesystem.
	 */
	@ParameterizedTest
	@CsvSource({ "../gizli.jpg", "ab/../../gizli.jpg", "ab/cd/../../../../etc/passwd" })
	void shouldRefuseAKeyThatClimbsOutOfTheRoot(String key) {
		assertThatThrownBy(() -> storage.read(key))
				.isInstanceOf(PhotoStorageException.class)
				.hasMessageContaining("Geçersiz dosya anahtarı");
		assertThatThrownBy(() -> storage.delete(key)).isInstanceOf(PhotoStorageException.class);
	}

	@Test
	void shouldNotWriteOutsideTheRoot() throws IOException {
		Path sibling = root.getParent().resolve("disarida.txt");
		Files.deleteIfExists(sibling);

		storage.store(CONTENT, "image/jpeg");

		assertThat(sibling).doesNotExist();
		try (var files = Files.walk(root)) {
			assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(1);
		}
	}
}
