package com.s2000coaching.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a member's logged sets say: best estimated one-rep max, whether the last session set a
 * record, weekly volume, and a plain-language suggestion for the next session.
 *
 * <p>Follows the engine's rule: where the data cannot support an answer it says so instead of
 * producing a number. An estimated 1RM is only taken from sets of 12 reps or fewer (the formula
 * is unreliable beyond that), a suggestion needs a numeric rep target and a logged weight, and a
 * suggestion resting on one session says it rests on one session.
 */
public final class TrainingStatsCalculator {

	/** Beyond this many reps a one-rep-max estimate is guesswork, so such sets are not used. */
	static final int MAX_REPS_FOR_1RM = 12;
	/** Average effort above this means the last session was already near the limit. */
	static final BigDecimal HARD_RPE = new BigDecimal("9.0");
	/** At or below this average effort, hitting the top of the rep range means "add weight". */
	static final BigDecimal EASY_ENOUGH_RPE = new BigDecimal("8.5");
	static final int VOLUME_WEEKS = 12;

	private static final Pattern REP_RANGE = Pattern.compile("^\\s*(\\d+)\\s*(?:[-–]\\s*(\\d+))?\\s*$");

	private TrainingStatsCalculator() {
	}

	public record SetFact(UUID exerciseId, String exerciseName, String prescribedReps, LocalDate date, int reps,
			BigDecimal weightKg, BigDecimal rpe) {
	}

	public record Best(BigDecimal estimatedOneRepMaxKg, BigDecimal weightKg, int reps, LocalDate on) {
	}

	/** kind is INCREASE, HOLD or INSUFFICIENT. */
	public record Suggestion(String kind, BigDecimal nextWeightKg, String text) {
	}

	/** The best estimated one-rep max of one session, for the progress chart. */
	public record ProgressPoint(LocalDate on, BigDecimal estimatedOneRepMaxKg) {
	}

	public record ExerciseStats(UUID exerciseId, String name, int sessions, LocalDate lastSessionOn,
			BigDecimal lastTopWeightKg, Best best, boolean newRecordInLastSession, Suggestion suggestion,
			List<ProgressPoint> progress) {
	}

	public record WeekVolume(LocalDate weekStart, int sets, BigDecimal volumeKg) {
	}

	public record Result(List<ExerciseStats> exercises, List<WeekVolume> weeklyVolume) {
	}

	public static Result calculate(List<SetFact> facts, LocalDate today) {
		Map<UUID, List<SetFact>> byExercise = new LinkedHashMap<>();
		for (SetFact fact : facts) {
			byExercise.computeIfAbsent(fact.exerciseId(), k -> new ArrayList<>()).add(fact);
		}
		List<ExerciseStats> exercises = byExercise.values().stream()
				.map(TrainingStatsCalculator::exerciseStats)
				.sorted(Comparator.comparing(ExerciseStats::lastSessionOn).reversed()
						.thenComparing(ExerciseStats::name))
				.toList();
		return new Result(exercises, weeklyVolume(facts, today));
	}

	private static ExerciseStats exerciseStats(List<SetFact> sets) {
		SetFact any = sets.get(0);
		TreeMap<LocalDate, List<SetFact>> bySession = new TreeMap<>();
		sets.forEach(set -> bySession.computeIfAbsent(set.date(), k -> new ArrayList<>()).add(set));
		LocalDate last = bySession.lastKey();
		List<SetFact> lastSets = bySession.get(last);

		Best best = null;
		Best bestBeforeLast = null;
		for (var entry : bySession.entrySet()) {
			for (SetFact set : entry.getValue()) {
				Best candidate = estimate(set);
				if (candidate == null) {
					continue;
				}
				if (best == null || candidate.estimatedOneRepMaxKg().compareTo(best.estimatedOneRepMaxKg()) > 0) {
					best = candidate;
				}
				if (!entry.getKey().equals(last) && (bestBeforeLast == null || candidate.estimatedOneRepMaxKg()
						.compareTo(bestBeforeLast.estimatedOneRepMaxKg()) > 0)) {
					bestBeforeLast = candidate;
				}
			}
		}
		// A record needs something to beat: the very first session sets a baseline, not a record.
		boolean record = best != null && best.on().equals(last) && bestBeforeLast != null
				&& best.estimatedOneRepMaxKg().compareTo(bestBeforeLast.estimatedOneRepMaxKg()) > 0;

		BigDecimal topWeight = lastSets.stream().map(SetFact::weightKg).filter(w -> w != null)
				.max(Comparator.naturalOrder()).orElse(null);
		// Prescription from the most recent set, so a changed rep target is the one used.
		String prescribed = lastSets.get(0).prescribedReps();
		// One point per session that had a usable weighted set: that session's best estimate, oldest first.
		List<ProgressPoint> progress = new ArrayList<>();
		for (var entry : bySession.entrySet()) {
			entry.getValue().stream().map(TrainingStatsCalculator::estimate).filter(e -> e != null)
					.max(Comparator.comparing(Best::estimatedOneRepMaxKg))
					.ifPresent(e -> progress.add(new ProgressPoint(entry.getKey(), e.estimatedOneRepMaxKg())));
		}
		return new ExerciseStats(any.exerciseId(), any.exerciseName(), bySession.size(), last, topWeight, best,
				record, suggest(lastSets, prescribed, bySession.size()), progress);
	}

	/** Epley: weight x (1 + reps / 30), and just the weight for a single. */
	static Best estimate(SetFact set) {
		if (set.weightKg() == null || set.weightKg().signum() <= 0 || set.reps() < 1
				|| set.reps() > MAX_REPS_FOR_1RM) {
			return null;
		}
		BigDecimal factor = set.reps() == 1 ? BigDecimal.ONE
				: BigDecimal.ONE.add(BigDecimal.valueOf(set.reps()).divide(BigDecimal.valueOf(30), 6, RoundingMode.HALF_UP));
		return new Best(set.weightKg().multiply(factor).setScale(1, RoundingMode.HALF_UP), set.weightKg(),
				set.reps(), set.date());
	}

	static Suggestion suggest(List<SetFact> lastSets, String prescribed, int sessions) {
		int[] range = parseRange(prescribed);
		if (range == null) {
			return new Suggestion("INSUFFICIENT", null,
					"Tekrar hedefi bir sayı ya da aralık olmadığı için ağırlık önerisi verilemiyor.");
		}
		List<SetFact> weighted = lastSets.stream()
				.filter(set -> set.weightKg() != null && set.weightKg().signum() > 0 && set.reps() > 0).toList();
		if (weighted.isEmpty()) {
			return new Suggestion("INSUFFICIENT", null,
					"Son seansta ağırlık girilmediği için öneri verilemiyor.");
		}
		BigDecimal top = weighted.stream().map(SetFact::weightKg).max(Comparator.naturalOrder()).orElseThrow();
		List<SetFact> working = weighted.stream().filter(set -> set.weightKg().compareTo(top) == 0).toList();
		int min = range[0];
		int max = range[1];
		BigDecimal avgRpe = average(working.stream().map(SetFact::rpe).filter(r -> r != null).toList());
		String basis = sessions == 1 ? " (yalnızca 1 seansa dayanıyor)" : "";

		boolean allAtTop = working.stream().allMatch(set -> set.reps() >= max);
		boolean anyBelow = working.stream().anyMatch(set -> set.reps() < min);

		if (avgRpe != null && avgRpe.compareTo(HARD_RPE) > 0) {
			return new Suggestion("HOLD", top,
					"Son seansı çok zor (ort. RPE %s) olarak işaretlediniz; %s kg ile devam edin%s."
							.formatted(avgRpe.toPlainString(), plain(top), basis));
		}
		if (allAtTop && (avgRpe == null || avgRpe.compareTo(EASY_ENOUGH_RPE) <= 0)) {
			BigDecimal next = top.add(top.compareTo(new BigDecimal("100")) >= 0 ? new BigDecimal("5") : new BigDecimal("2.5"));
			return new Suggestion("INCREASE", next,
					"Çalışma setlerinin hepsinde üst hedefi (%d tekrar) yaptınız; sonraki seans %s kg deneyin%s."
							.formatted(max, plain(next), basis));
		}
		if (anyBelow) {
			return new Suggestion("HOLD", top,
					"Bazı setlerde alt hedefin (%d) altında kaldınız; %s kg ile tekrarı yükseltin%s."
							.formatted(min, plain(top), basis));
		}
		return new Suggestion("HOLD", top,
				"Tekrar aralığındasınız; üst hedefe (%d) ulaşana kadar %s kg ile devam edin%s."
						.formatted(max, plain(top), basis));
	}

	static int[] parseRange(String prescribed) {
		if (prescribed == null) {
			return null;
		}
		Matcher matcher = REP_RANGE.matcher(prescribed);
		if (!matcher.matches()) {
			return null;
		}
		int low = Integer.parseInt(matcher.group(1));
		int high = matcher.group(2) == null ? low : Integer.parseInt(matcher.group(2));
		return low >= 1 && high >= low ? new int[] {low, high} : null;
	}

	private static List<WeekVolume> weeklyVolume(List<SetFact> facts, LocalDate today) {
		LocalDate thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate first = thisWeek.minusWeeks(VOLUME_WEEKS - 1L);
		TreeMap<LocalDate, int[]> sets = new TreeMap<>();
		TreeMap<LocalDate, BigDecimal> volume = new TreeMap<>();
		for (int i = 0; i < VOLUME_WEEKS; i++) {
			sets.put(first.plusWeeks(i), new int[1]);
			volume.put(first.plusWeeks(i), BigDecimal.ZERO);
		}
		for (SetFact fact : facts) {
			LocalDate week = fact.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			if (!sets.containsKey(week) || fact.reps() < 1) {
				continue;
			}
			sets.get(week)[0]++;
			if (fact.weightKg() != null) {
				volume.merge(week, fact.weightKg().multiply(BigDecimal.valueOf(fact.reps())), BigDecimal::add);
			}
		}
		List<WeekVolume> result = new ArrayList<>();
		sets.forEach((week, count) -> result.add(new WeekVolume(week, count[0], volume.get(week).setScale(0, RoundingMode.HALF_UP))));
		return result;
	}

	private static BigDecimal average(List<BigDecimal> values) {
		if (values.isEmpty()) {
			return null;
		}
		return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
	}

	private static String plain(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}
}
