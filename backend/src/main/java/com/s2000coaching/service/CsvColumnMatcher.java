package com.s2000coaching.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guesses which CSV column holds which field, so the coach confirms a filled-in mapping
 * instead of building one from scratch.
 *
 * <p>Only a guess: every match is shown back for approval before anything is imported. The
 * alternative — importing on the guess alone — would quietly put the wrong column into a
 * published figure, and nobody would know to look.
 */
public final class CsvColumnMatcher {

	/** Canonical field name to the header spellings seen in Turkish and English exports. */
	private static final Map<String, List<String>> MEASUREMENT_ALIASES = new LinkedHashMap<>();
	private static final Map<String, List<String>> INTAKE_ALIASES = new LinkedHashMap<>();

	static {
		MEASUREMENT_ALIASES.put("measuredOn", List.of("tarih", "date", "gun", "day", "olcumtarihi"));
		MEASUREMENT_ALIASES.put("weightKg", List.of("kilo", "agirlik", "weight", "kg", "bodyweight", "vucutagirligi"));
		MEASUREMENT_ALIASES.put("neckCm", List.of("boyun", "neck"));
		MEASUREMENT_ALIASES.put("waistCm", List.of("bel", "waist"));
		MEASUREMENT_ALIASES.put("chestCm", List.of("gogus", "chest"));
		MEASUREMENT_ALIASES.put("hipCm", List.of("kalca", "hip", "hips"));
		MEASUREMENT_ALIASES.put("armCm", List.of("kol", "arm", "biceps", "pazu"));
		MEASUREMENT_ALIASES.put("thighCm", List.of("bacak", "thigh", "uyluk", "leg"));

		INTAKE_ALIASES.put("consumedOn", List.of("tarih", "date", "gun", "day"));
		INTAKE_ALIASES.put("calories", List.of("kalori", "calories", "kcal", "energy", "enerji", "kalorialimi"));
		INTAKE_ALIASES.put("proteinG", List.of("protein", "proteing", "proteingram"));
		INTAKE_ALIASES.put("carbsG", List.of("karbonhidrat", "karb", "carbs", "carbohydrate", "carbohydrates", "kh"));
		INTAKE_ALIASES.put("fatG", List.of("yag", "fat", "fats", "yagg"));
		INTAKE_ALIASES.put("sodiumMg", List.of("sodyum", "sodium", "tuz", "salt", "na"));
	}

	private CsvColumnMatcher() {
	}

	public static Map<String, Integer> guessMeasurementColumns(List<String> headers) {
		return guess(headers, MEASUREMENT_ALIASES);
	}

	public static Map<String, Integer> guessIntakeColumns(List<String> headers) {
		return guess(headers, INTAKE_ALIASES);
	}

	public static List<String> measurementFields() {
		return List.copyOf(MEASUREMENT_ALIASES.keySet());
	}

	public static List<String> intakeFields() {
		return List.copyOf(INTAKE_ALIASES.keySet());
	}

	/**
	 * Exact matches are taken first across all fields, and only then are the leftovers
	 * considered for a prefix match. Done field by field instead, a loose match on an early
	 * field could claim the column that a later field matches exactly — "yag" (fat) would
	 * take "yagorani" (fat percentage) before the real fat column was reached.
	 */
	private static Map<String, Integer> guess(List<String> headers, Map<String, List<String>> aliases) {
		List<String> normalized = headers.stream().map(CsvParser::normalizeHeader).toList();
		Map<String, Integer> mapping = new LinkedHashMap<>();
		boolean[] taken = new boolean[normalized.size()];

		for (var entry : aliases.entrySet()) {
			for (int i = 0; i < normalized.size(); i++) {
				if (!taken[i] && entry.getValue().contains(normalized.get(i))) {
					mapping.put(entry.getKey(), i);
					taken[i] = true;
					break;
				}
			}
		}

		for (var entry : aliases.entrySet()) {
			if (mapping.containsKey(entry.getKey())) {
				continue;
			}
			for (int i = 0; i < normalized.size(); i++) {
				if (taken[i] || normalized.get(i).isEmpty()) {
					continue;
				}
				String header = normalized.get(i);
				boolean matches = entry.getValue().stream()
						.anyMatch(alias -> header.startsWith(alias) || header.contains(alias));
				if (matches) {
					mapping.put(entry.getKey(), i);
					taken[i] = true;
					break;
				}
			}
		}
		return mapping;
	}
}
