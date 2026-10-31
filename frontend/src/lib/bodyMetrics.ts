import type { MonthlyRecord } from '@/types/monthlyRecord'
import type { MemberGender } from '@/types/member'

/**
 * Figures worked out from a month's circumferences. Body fat is an ESTIMATE by the US Navy tape method
 * (neck, waist, and hip for women, against height); it is not a measurement, and the screens that show
 * it say so. Lean mass and FFMI follow from it, so they carry the same uncertainty.
 */
export interface DerivedMetrics {
  bodyFatPercent: number | null
  leanMassPercent: number | null
  leanMassKg: number | null
  /** Fat-free mass index, kg/m². */
  ffmi: number | null
  /** FFMI adjusted to a 1.80 m height (Kouri et al.), which makes tall and short people comparable. */
  normalizedFfmi: number | null
  /** Shoulder circumference over waist circumference. */
  shoulderWaistRatio: number | null
}

const EMPTY: DerivedMetrics = {
  bodyFatPercent: null,
  leanMassPercent: null,
  leanMassKg: null,
  ffmi: null,
  normalizedFfmi: null,
  shoulderWaistRatio: null,
}

/** Navy body-fat estimate in percent, or null when a needed figure is missing or the tape figures make no sense. */
export function navyBodyFat(
  gender: MemberGender | null,
  heightCm: number | null,
  neckCm: number | null,
  waistCm: number | null,
  hipCm: number | null,
): number | null {
  if (heightCm === null || neckCm === null || waistCm === null) {
    return null
  }
  let percent: number
  if (gender === 'MALE') {
    if (waistCm <= neckCm) {
      return null
    }
    percent = 86.01 * Math.log10(waistCm - neckCm) - 70.041 * Math.log10(heightCm) + 36.76
  } else if (gender === 'FEMALE') {
    if (hipCm === null || waistCm + hipCm <= neckCm) {
      return null
    }
    percent = 163.205 * Math.log10(waistCm + hipCm - neckCm) - 97.684 * Math.log10(heightCm) - 78.387
  } else {
    // The formula exists for two sexes only; for anyone else it would be a guess dressed as a number.
    return null
  }
  return percent > 2 && percent < 60 ? percent : null
}

/** What a month's record yields, using the member's profile height when the table has none. */
export function derive(record: MonthlyRecord, gender: MemberGender | null, profileHeightCm: number | null): DerivedMetrics {
  const height = record.heightCm ?? profileHeightCm
  const result: DerivedMetrics = { ...EMPTY }

  if (record.shoulderCm !== null && record.waistCm !== null && record.waistCm > 0) {
    result.shoulderWaistRatio = record.shoulderCm / record.waistCm
  }

  const fat = navyBodyFat(gender, height, record.neckCm, record.waistCm, record.hipCm)
  if (fat !== null) {
    result.bodyFatPercent = fat
    result.leanMassPercent = 100 - fat
    if (record.weightKg !== null && height !== null) {
      const lean = record.weightKg * (1 - fat / 100)
      const meters = height / 100
      result.leanMassKg = lean
      result.ffmi = lean / (meters * meters)
      result.normalizedFfmi = result.ffmi + 6.1 * (1.8 - meters)
    }
  }
  return result
}
