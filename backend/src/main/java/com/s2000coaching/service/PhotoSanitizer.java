package com.s2000coaching.service;

import com.s2000coaching.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Makes an uploaded photo safe to publish: decodes it and writes it back out as a bare image.
 *
 * <p>A phone photo carries EXIF — GPS position, device, timestamp — and a file that is shown
 * to the whole internet must not. Re-encoding drops every metadata block, and it also proves
 * the bytes really are an image rather than whatever the client claimed in its content type.
 * The one piece of EXIF that matters for display, the orientation, is applied to the pixels
 * first; otherwise a portrait shot would come out lying on its side.
 *
 * <p>JPEG and PNG only: the JDK has no WebP decoder, and a format that cannot be decoded
 * cannot be cleaned, so it is refused rather than published with its metadata intact.
 */
public final class PhotoSanitizer {

	/** Guards against a small file that decodes to a huge bitmap and exhausts memory. */
	static final long MAX_PIXELS = 50_000_000L;

	private static final float JPEG_QUALITY = 0.92f;

	private PhotoSanitizer() {
	}

	public record Sanitized(byte[] content, String contentType) {
	}

	public static Sanitized sanitize(byte[] original) {
		String format = detectFormat(original);
		BufferedImage decoded = decode(original, format);

		int orientation = "jpeg".equals(format) ? exifOrientation(original) : 1;
		BufferedImage upright = applyOrientation(decoded, orientation);

		try {
			return "jpeg".equals(format)
					? new Sanitized(encodeJpeg(upright), "image/jpeg")
					: new Sanitized(encodePng(upright), "image/png");
		} catch (IOException e) {
			throw new BadRequestException("Fotoğraf işlenemedi.");
		}
	}

	private static String detectFormat(byte[] bytes) {
		if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
			return "jpeg";
		}
		if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N'
				&& bytes[3] == 'G') {
			return "png";
		}
		throw new BadRequestException(
				"Vaka fotoğrafı yalnızca JPEG veya PNG olabilir; WebP temizlenemediği için kabul edilmez.");
	}

	private static BufferedImage decode(byte[] bytes, String format) {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format);
			if (input == null || !readers.hasNext()) {
				throw new BadRequestException("Fotoğraf okunamadı.");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
				if (pixels > MAX_PIXELS) {
					throw new BadRequestException("Fotoğrafın çözünürlüğü çok yüksek.");
				}
				return reader.read(0);
			} finally {
				reader.dispose();
			}
		} catch (IOException | RuntimeException e) {
			if (e instanceof BadRequestException bad) {
				throw bad;
			}
			// Includes CMYK JPEGs, which the JDK reader refuses.
			throw new BadRequestException("Fotoğraf okunamadı; dosya bozuk ya da desteklenmeyen bir türde.");
		}
	}

	/** EXIF orientation 1..8, or 1 when there is none or it cannot be read. */
	static int exifOrientation(byte[] jpeg) {
		int pos = 2;
		while (pos + 4 <= jpeg.length) {
			if ((jpeg[pos] & 0xFF) != 0xFF) {
				return 1;
			}
			int marker = jpeg[pos + 1] & 0xFF;
			if (marker == 0xD9 || marker == 0xDA) {
				return 1; // end of image or start of scan: no more headers
			}
			int length = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);
			if (length < 2) {
				return 1;
			}
			if (marker == 0xE1 && pos + 4 + 6 <= jpeg.length && jpeg[pos + 4] == 'E'
					&& jpeg[pos + 5] == 'x' && jpeg[pos + 6] == 'i' && jpeg[pos + 7] == 'f') {
				return tiffOrientation(jpeg, pos + 10, Math.min(jpeg.length, pos + 2 + length));
			}
			pos += 2 + length;
		}
		return 1;
	}

	private static int tiffOrientation(byte[] b, int tiff, int end) {
		if (tiff + 8 > end) {
			return 1;
		}
		boolean little = b[tiff] == 'I' && b[tiff + 1] == 'I';
		if (!little && !(b[tiff] == 'M' && b[tiff + 1] == 'M')) {
			return 1;
		}
		long ifd = u32(b, tiff + 4, little);
		int entries = (int) (ifd + 2 <= end - tiff ? u16(b, tiff + (int) ifd, little) : 0);
		for (int i = 0; i < entries; i++) {
			int entry = tiff + (int) ifd + 2 + i * 12;
			if (entry + 12 > end) {
				return 1;
			}
			if (u16(b, entry, little) == 0x0112) {
				int value = u16(b, entry + 8, little);
				return value >= 1 && value <= 8 ? value : 1;
			}
		}
		return 1;
	}

	private static int u16(byte[] b, int at, boolean little) {
		int a = b[at] & 0xFF;
		int c = b[at + 1] & 0xFF;
		return little ? (c << 8) | a : (a << 8) | c;
	}

	private static long u32(byte[] b, int at, boolean little) {
		long hi = u16(b, little ? at + 2 : at, little);
		long lo = u16(b, little ? at : at + 2, little);
		return (hi << 16) | lo;
	}

	/** Moves each pixel to where the orientation tag says it belongs. */
	static BufferedImage applyOrientation(BufferedImage src, int orientation) {
		int w = src.getWidth();
		int h = src.getHeight();
		boolean swaps = orientation >= 5;
		int nw = swaps ? h : w;
		int nh = swaps ? w : h;

		BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
		if (orientation == 1) {
			out.getGraphics().drawImage(src, 0, 0, null);
			return out;
		}

		int[] in = src.getRGB(0, 0, w, h, null, 0, w);
		int[] dst = new int[nw * nh];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int nx;
				int ny;
				switch (orientation) {
					case 2 -> { nx = w - 1 - x; ny = y; }
					case 3 -> { nx = w - 1 - x; ny = h - 1 - y; }
					case 4 -> { nx = x; ny = h - 1 - y; }
					case 5 -> { nx = y; ny = x; }
					case 6 -> { nx = h - 1 - y; ny = x; }
					case 7 -> { nx = h - 1 - y; ny = w - 1 - x; }
					default -> { nx = y; ny = w - 1 - x; }
				}
				dst[ny * nw + nx] = in[y * w + x];
			}
		}
		out.setRGB(0, 0, nw, nh, dst, 0, nw);
		return out;
	}

	private static byte[] encodeJpeg(BufferedImage image) throws IOException {
		// JPEG has no alpha channel; flatten onto white rather than let the writer fail.
		BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		var g = rgb.createGraphics();
		g.setColor(java.awt.Color.WHITE);
		g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
		g.drawImage(image, 0, 0, null);
		g.dispose();

		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		try {
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
				writer.setOutput(stream);
				// No metadata argument: the output carries none of the original's.
				writer.write(null, new IIOImage(rgb, null, null), param);
			}
			return out.toByteArray();
		} finally {
			writer.dispose();
		}
	}

	private static byte[] encodePng(BufferedImage image) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (!ImageIO.write(image, "png", out)) {
			throw new IOException("no png writer");
		}
		return out.toByteArray();
	}
}
