package com.s2000coaching.service.analysis;

import com.s2000coaching.domain.Sex;
import com.s2000coaching.service.analysis.BodyAnalysis.BodyComposition;
import java.time.LocalDate;

/**
 * Estimates body fat from tape measurements. Every result says which formula produced it and
 * how wide the band around it is.
 *
 * <p>This is worth stating plainly because the number is the one people quote: body fat
 * cannot be measured with a tape, only estimated, and both formulas here sit within roughly
 * three to five percentage points of a DEXA scan at best. A figure printed bare would be
 * read as fact, so nothing here returns one without its method and its uncertainty.
 *
 * <p>Muscle mass is not estimated separately. Lean mass is whatever is not fat, so it
 * inherits the same error — presenting it as a measured quantity would hide that.
 */
public final class BodyCompositionEstimator {

	/** Navy circumference method, against DEXA. */
	private static final double NAVY_UNCERTAINTY_POINTS = 3.5;

	/** Deurenberg works from BMI, so it cannot tell muscle from fat and is wider. */
	private static final double DEURENBERG_UNCERTAINTY_POINTS = 5.0;

	private BodyCompositionEstimator() {
	}

	/**
	 * Picks the best formula the data supports, or returns a result with a null percentage
	 * naming what is missing. Guessing from fewer inputs would be worse than saying nothing.
	 */
	/**
	 * A reading taken with a scale or a caliper. Narrower than the formulas but not exact:
	 * a bioimpedance scale moves with hydration, a caliper with whoever is holding it, so it
	 * carries an uncertainty like everything else here.
	 */
	private static final double MEASURED_UNCERTAINTY_POINTS = 2.5;

	/**
	 * What was measured, rather than what a formula infers. The caller passes this only when
	 * the record carries a body-fat reading of its own.
	 */
	public static BodyComposition fromMeasurement(LocalDate asOf, double weightKg, double percent) {
		return build("Ölçüm kaydı", percent, weightKg, MEASURED_UNCERTAINTY_POINTS, asOf,
				"Kaydın kendi yağ oranı ölçümünden alındı, formülle tahmin edilmedi. Tartı ya da "
						+ "kaliper ölçümü su tutulumundan ve ölçen kişiden etkilenir.");
	}

	public static BodyComposition estimate(LocalDate asOf, Double weightKg, Integer heightCm, Sex sex,
			Integer birthYear, Double neckCm, Double waistCm, Double hipCm, int currentYear) {
		if (weightKg == null) {
			return unavailable(asOf, "Kilo verisi yok.");
		}

		if (heightCm != null && sex != null && neckCm != null && waistCm != null
				&& (sex == Sex.ERKEK || hipCm != null)) {
			Double percent = navy(sex, heightCm, neckCm, waistCm, hipCm);
			if (percent != null) {
				return build("US Navy (çevre ölçümü)", percent, weightKg, NAVY_UNCERTAINTY_POINTS, asOf,
						"Boyun, bel ve boy ölçüsünden hesaplandı. DEXA taramasına kıyasla yaklaşık "
								+ "±3,5 puan sapar.");
			}
		}

		if (heightCm != null && sex != null && birthYear != null) {
			int age = currentYear - birthYear;
			double bmi = weightKg / Math.pow(heightCm / 100.0, 2);
			double percent = 1.20 * bmi + 0.23 * age - 10.8 * (sex == Sex.ERKEK ? 1 : 0) - 5.4;
			return build("Deurenberg (BKİ tabanlı)", percent, weightKg, DEURENBERG_UNCERTAINTY_POINTS, asOf,
					"Boyun ölçüsü olmadığı için BKİ tabanlı formül kullanıldı. BKİ kas ile yağı "
							+ "ayırt edemediği için kaslı kişilerde yağ oranını yüksek gösterir; "
							+ "sapma ±5 puana çıkar. Boyun ölçüsü eklenirse Navy formülüne geçilir.");
		}

		return unavailable(asOf, missingInputs(heightCm, sex, birthYear, neckCm, waistCm));
	}

	/**
	 * Navy method, metric form. Returns null when the logarithm's argument is not positive —
	 * a waist at or below the neck is a typo, not a body, and must not produce a number.
	 */
	private static Double navy(Sex sex, int heightCm, double neckCm, double waistCm, Double hipCm) {
		if (sex == Sex.ERKEK) {
			double girth = waistCm - neckCm;
			if (girth <= 0) {
				return null;
			}
			return 495 / (1.0324 - 0.19077 * Math.log10(girth) + 0.15456 * Math.log10(heightCm)) - 450;
		}

		double girth = waistCm + hipCm - neckCm;
		if (girth <= 0) {
			return null;
		}
		return 495 / (1.29579 - 0.35004 * Math.log10(girth) + 0.22100 * Math.log10(heightCm)) - 450;
	}

	private static BodyComposition build(String method, double percent, double weightKg,
			double uncertainty, LocalDate asOf, String note) {
		// Outside this range the formula has left the data it was fitted on; the number would
		// be arithmetic rather than an estimate.
		if (percent < 3 || percent > 60) {
			return unavailable(asOf, "Hesaplanan oran (%%%.1f) makul aralığın dışında; ölçümlerde "
					.formatted(percent) + "hata olabilir.");
		}

		double fatMass = weightKg * percent / 100.0;
		return new BodyComposition(method, TrendCalculator.round(percent, 1),
				TrendCalculator.round(weightKg - fatMass, 1), TrendCalculator.round(fatMass, 1),
				uncertainty, asOf, note);
	}

	private static BodyComposition unavailable(LocalDate asOf, String reason) {
		return new BodyComposition(null, null, null, null, 0, asOf, reason);
	}

	private static String missingInputs(Integer heightCm, Sex sex, Integer birthYear, Double neckCm,
			Double waistCm) {
		StringBuilder missing = new StringBuilder("Yağ oranı tahmini için eksik veri: ");
		boolean first = true;
		if (heightCm == null) {
			missing.append("boy");
			first = false;
		}
		if (sex == null) {
			missing.append(first ? "cinsiyet" : ", cinsiyet");
			first = false;
		}
		if (neckCm == null && waistCm == null) {
			missing.append(first ? "boyun ve bel ölçüsü" : ", boyun ve bel ölçüsü");
		} else if (neckCm == null) {
			missing.append(first ? "boyun ölçüsü" : ", boyun ölçüsü");
		} else if (birthYear == null) {
			missing.append(first ? "doğum yılı" : ", doğum yılı");
		}
		return missing.append(".").toString();
	}
}
