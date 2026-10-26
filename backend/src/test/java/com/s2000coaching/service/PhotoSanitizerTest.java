package com.s2000coaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.s2000coaching.exception.BadRequestException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PhotoSanitizerTest {

	private static final String SECRET = "GPSLatitude41.0082N";

	private static byte[] image(String format, int w, int h, Color color) throws Exception {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		var g = img.createGraphics();
		g.setColor(color);
		g.fillRect(0, 0, w, h);
		g.dispose();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, format, out);
		return out.toByteArray();
	}

	/** Inserts an EXIF APP1 right after SOI, carrying an orientation tag and a "GPS" string. */
	private static byte[] withExif(byte[] jpeg, int orientation) {
		byte[] secret = SECRET.getBytes(StandardCharsets.US_ASCII);
		ByteArrayOutputStream tiff = new ByteArrayOutputStream();
		byte[] header = {'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0,
				(byte) orientation, 0, 0, 0, 0, 0, 0, 0};
		tiff.writeBytes(header);
		tiff.writeBytes(secret);
		ByteArrayOutputStream app1 = new ByteArrayOutputStream();
		app1.writeBytes(new byte[] {'E', 'x', 'i', 'f', 0, 0});
		app1.writeBytes(tiff.toByteArray());
		int length = app1.size() + 2;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(jpeg, 0, 2);
		out.write(0xFF);
		out.write(0xE1);
		out.write(length >> 8);
		out.write(length & 0xFF);
		out.writeBytes(app1.toByteArray());
		out.write(jpeg, 2, jpeg.length - 2);
		return out.toByteArray();
	}

	private static boolean contains(byte[] haystack, String needle) {
		return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle);
	}

	@Test
	void shouldDropExifIncludingLocationData() throws Exception {
		byte[] original = withExif(image("jpeg", 40, 20, Color.RED), 1);
		assertThat(contains(original, SECRET)).isTrue();

		var clean = PhotoSanitizer.sanitize(original);

		assertThat(contains(clean.content(), SECRET)).isFalse();
		assertThat(contains(clean.content(), "Exif")).isFalse();
		assertThat(clean.contentType()).isEqualTo("image/jpeg");
	}

	/** A portrait shot stored sideways with an orientation tag must not come out lying down. */
	@Test
	void shouldApplyExifOrientationToThePixels() throws Exception {
		byte[] landscapeWithRotateTag = withExif(image("jpeg", 40, 20, Color.BLUE), 6);

		var clean = PhotoSanitizer.sanitize(landscapeWithRotateTag);

		BufferedImage result = ImageIO.read(new ByteArrayInputStream(clean.content()));
		assertThat(result.getWidth()).isEqualTo(20);
		assertThat(result.getHeight()).isEqualTo(40);
	}

	@Test
	void shouldMoveEachCornerWhereTheOrientationSaysForRotate90() {
		BufferedImage src = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
		src.setRGB(0, 0, 0xFFFF0000); // top-left red

		BufferedImage rotated = PhotoSanitizer.applyOrientation(src, 6); // 90 degrees clockwise

		// Top-left of a clockwise-rotated image ends up top-right.
		assertThat(rotated.getWidth()).isEqualTo(2);
		assertThat(rotated.getRGB(1, 0)).isEqualTo(0xFFFF0000);
	}

	@Test
	void shouldReadOrientationFromBothByteOrders() throws Exception {
		assertThat(PhotoSanitizer.exifOrientation(withExif(image("jpeg", 8, 8, Color.RED), 8))).isEqualTo(8);
		assertThat(PhotoSanitizer.exifOrientation(image("jpeg", 8, 8, Color.RED))).isEqualTo(1);
	}

	@Test
	void shouldKeepPngsAsPng() throws Exception {
		var clean = PhotoSanitizer.sanitize(image("png", 10, 10, Color.GREEN));

		assertThat(clean.contentType()).isEqualTo("image/png");
		assertThat(ImageIO.read(new ByteArrayInputStream(clean.content()))).isNotNull();
	}

	/** The declared content type is never trusted; the bytes have to decode as an image. */
	@Test
	void shouldRejectBytesThatAreNotAnImage() {
		byte[] text = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> PhotoSanitizer.sanitize(text)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldRejectATruncatedJpeg() throws Exception {
		byte[] jpeg = image("jpeg", 40, 20, Color.RED);
		byte[] truncated = java.util.Arrays.copyOf(jpeg, 30);

		assertThatThrownBy(() -> PhotoSanitizer.sanitize(truncated)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void shouldRefuseWebpBecauseItCannotBeCleaned() {
		byte[] webp = "RIFF....WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

		assertThatThrownBy(() -> PhotoSanitizer.sanitize(webp))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("WebP");
	}
}
