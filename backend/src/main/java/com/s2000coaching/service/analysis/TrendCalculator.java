package com.s2000coaching.service.analysis;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Smoothing and the small statistics the analysis leans on. */
public final class TrendCalculator {

	/** Per day. Roughly a one-week half-life, which is what hides water-weight swings. */
	public static final double DAILY_ALPHA = 0.1;

	private TrendCalculator() {
	}

	public record Observation(LocalDate date, double value) {
	}

	/**
	 * Exponentially smoothed series, with the weight of each step scaled by how long it has
	 * been since the previous reading.
	 *
	 * <p>A plain EMA assumes evenly spaced points. Run over weigh-ins that are sometimes
	 * daily and sometimes weekly — which is what a two-year record looks like — it would
	 * treat a week-long gap as one day and lag the real trend badly. Scaling alpha by the
	 * gap ({@code 1 - (1-α)^days}) makes a seven-day gap apply seven days of smoothing, so
	 * the same code is right for both cadences.
	 */
	public static List<Observation> exponentialTrend(List<Observation> points, double dailyAlpha) {
		List<Observation> trend = new ArrayList<>();
		if (points.isEmpty()) {
			return trend;
		}

		double current = points.get(0).value();
		trend.add(new Observation(points.get(0).date(), current));

		for (int i = 1; i < points.size(); i++) {
			Observation point = points.get(i);
			long gapDays = Math.max(1, ChronoUnit.DAYS.between(points.get(i - 1).date(), point.date()));
			double effectiveAlpha = 1 - Math.pow(1 - dailyAlpha, gapDays);
			current = effectiveAlpha * point.value() + (1 - effectiveAlpha) * current;
			trend.add(new Observation(point.date(), current));
		}
		return trend;
	}

	/**
	 * Pearson correlation, or null when there is too little to say. Four points can produce
	 * a correlation of 0.95 out of noise, so anything below six is reported as "not enough"
	 * rather than as a number someone might quote.
	 */
	public static Double correlation(List<Double> xs, List<Double> ys) {
		if (xs.size() != ys.size() || xs.size() < 6) {
			return null;
		}
		int n = xs.size();
		double meanX = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double meanY = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0);

		double covariance = 0;
		double varianceX = 0;
		double varianceY = 0;
		for (int i = 0; i < n; i++) {
			double dx = xs.get(i) - meanX;
			double dy = ys.get(i) - meanY;
			covariance += dx * dy;
			varianceX += dx * dx;
			varianceY += dy * dy;
		}
		if (varianceX == 0 || varianceY == 0) {
			return null;
		}
		return covariance / Math.sqrt(varianceX * varianceY);
	}

	/** Least-squares slope of y on x, or null when x never varies. */
	public static Double slope(List<Double> xs, List<Double> ys) {
		if (xs.size() != ys.size() || xs.size() < 2) {
			return null;
		}
		double meanX = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
		double meanY = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0);

		double numerator = 0;
		double denominator = 0;
		for (int i = 0; i < xs.size(); i++) {
			double dx = xs.get(i) - meanX;
			numerator += dx * (ys.get(i) - meanY);
			denominator += dx * dx;
		}
		return denominator == 0 ? null : numerator / denominator;
	}

	public static double round(double value, int decimals) {
		double factor = Math.pow(10, decimals);
		return Math.round(value * factor) / factor;
	}
}
