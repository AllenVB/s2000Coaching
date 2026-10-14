package com.s2000coaching.service;

import com.s2000coaching.exception.BadRequestException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small CSV reader for imported food and measurement logs.
 *
 * <p>Deliberately not a general CSV library. What arrives here is an export from a tracking
 * app or a spreadsheet, and the few things that actually differ between those — the
 * separator, the decimal mark, the date order, quoted fields containing the separator — are
 * handled below. Anything stranger is rejected with a message naming the row, because a
 * silently misread number would travel all the way into a published body-fat figure.
 */
public final class CsvParser {

	/** Turkish exports use ';' with ',' as the decimal mark; English ones use ',' with '.'. */
	private static final char[] CANDIDATE_SEPARATORS = { ',', ';', '\t' };

	private static final DateTimeFormatter[] DATE_FORMATS = {
			DateTimeFormatter.ISO_LOCAL_DATE,
			DateTimeFormatter.ofPattern("dd.MM.yyyy"),
			DateTimeFormatter.ofPattern("dd/MM/yyyy"),
			DateTimeFormatter.ofPattern("MM/dd/yyyy"),
			DateTimeFormatter.ofPattern("yyyy/MM/dd"),
			DateTimeFormatter.ofPattern("d.M.yyyy"),
	};

	private CsvParser() {
	}

	/** Header names plus every data row, with cells trimmed and the BOM removed. */
	public record Table(List<String> headers, List<List<String>> rows, char separator) {

		public int columnCount() {
			return headers.size();
		}
	}

	public static Table parse(byte[] content) {
		if (content.length == 0) {
			throw new BadRequestException("Dosya boş.");
		}

		List<String> lines = readLines(content);
		if (lines.isEmpty()) {
			throw new BadRequestException("Dosyada satır bulunamadı.");
		}

		char separator = detectSeparator(lines.get(0));
		List<String> headers = splitLine(lines.get(0), separator);
		if (headers.size() < 2) {
			throw new BadRequestException(
					"Başlık satırı okunamadı. Dosyanın ilk satırı sütun adlarını içermeli.");
		}

		List<List<String>> rows = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.isBlank()) {
				continue;
			}
			List<String> cells = splitLine(line, separator);
			// Short rows are padded rather than rejected: trailing empty columns are routinely
			// dropped by spreadsheet exports, and the row is still usable.
			while (cells.size() < headers.size()) {
				cells.add("");
			}
			rows.add(cells);
		}
		return new Table(headers, rows, separator);
	}

	private static List<String> readLines(byte[] content) {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
			String line;
			boolean first = true;
			while ((line = reader.readLine()) != null) {
				if (first) {
					// Excel writes a byte-order mark; left in place it becomes part of the first
					// header name and that column never matches anything.
					line = line.replace("﻿", "");
					first = false;
				}
				lines.add(line);
			}
		} catch (IOException e) {
			throw new BadRequestException("Dosya okunamadı.");
		}
		return lines;
	}

	/** The separator that splits the header into the most columns wins. */
	private static char detectSeparator(String headerLine) {
		char best = ',';
		int bestCount = 0;
		for (char candidate : CANDIDATE_SEPARATORS) {
			int count = splitLine(headerLine, candidate).size();
			if (count > bestCount) {
				bestCount = count;
				best = candidate;
			}
		}
		return best;
	}

	/** Handles quoted cells, so a quoted "1.234,5" keeps its separator. */
	private static List<String> splitLine(String line, char separator) {
		List<String> cells = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				// A doubled quote inside a quoted cell is one literal quote.
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == separator && !inQuotes) {
				cells.add(current.toString().trim());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		cells.add(current.toString().trim());
		return cells;
	}

	/** @return null for an empty cell; callers decide whether that column was required. */
	public static LocalDate parseDate(String value, int rowNumber) {
		if (value == null || value.isBlank()) {
			return null;
		}
		// Some exports append a time; the date is all this needs.
		String trimmed = value.trim().split("[ T]")[0];
		for (DateTimeFormatter format : DATE_FORMATS) {
			try {
				return LocalDate.parse(trimmed, format);
			} catch (Exception ignored) {
				// Try the next shape.
			}
		}
		throw new BadRequestException(
				"%d. satırdaki tarih okunamadı: \"%s\". Beklenen biçimler: 2026-03-15, 15.03.2026, 15/03/2026."
						.formatted(rowNumber, value));
	}

	/**
	 * Accepts both decimal marks. "1.234,5" and "1,234.5" both mean the same number, and
	 * guessing wrong turns 1234.5 kg into 1.2345 — so the rule is explicit: whichever of
	 * ',' and '.' appears last is the decimal mark, and the other is a thousands separator.
	 */
	public static BigDecimal parseDecimal(String value, int rowNumber, String columnLabel) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String cleaned = value.trim().replace(" ", "").replace(" ", "");
		int lastComma = cleaned.lastIndexOf(',');
		int lastDot = cleaned.lastIndexOf('.');

		if (lastComma >= 0 && lastDot >= 0) {
			if (lastComma > lastDot) {
				cleaned = cleaned.replace(".", "").replace(',', '.');
			} else {
				cleaned = cleaned.replace(",", "");
			}
		} else if (lastComma >= 0) {
			// A lone comma is a decimal mark unless it groups three digits ("1,234").
			boolean looksLikeThousands = cleaned.length() - lastComma == 4;
			cleaned = looksLikeThousands ? cleaned.replace(",", "") : cleaned.replace(',', '.');
		}

		try {
			return new BigDecimal(cleaned);
		} catch (NumberFormatException e) {
			throw new BadRequestException("%d. satırda \"%s\" sayıya çevrilemedi: \"%s\"."
					.formatted(rowNumber, columnLabel, value));
		}
	}

	public static Integer parseInteger(String value, int rowNumber, String columnLabel) {
		BigDecimal decimal = parseDecimal(value, rowNumber, columnLabel);
		return decimal == null ? null : decimal.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
	}

	/** Lower-cased with Turkish letters folded, so "Kalori" and "KALORİ" match one key. */
	public static String normalizeHeader(String header) {
		return header.toLowerCase(new Locale("tr"))
				.replace('ı', 'i')
				.replace('ş', 's')
				.replace('ğ', 'g')
				.replace('ü', 'u')
				.replace('ö', 'o')
				.replace('ç', 'c')
				.replaceAll("[^a-z0-9]", "");
	}
}
