/**
 * The estimates behind the public calculators. Everything here is a textbook formula applied to
 * what the visitor typed; none of it is stored or sent anywhere, and every result is a starting
 * point, not a prescription.
 */

export type Sex = 'FEMALE' | 'MALE'

export interface Body {
  sex: Sex
  ageYears: number
  heightCm: number
  weightKg: number
}

/** Bounds the form accepts; the same ones the member sign-up uses for height and age. */
export const BODY_LIMITS = {
  age: { min: 14, max: 100 },
  height: { min: 100, max: 250 },
  weight: { min: 30, max: 300 },
} as const

/** Basal metabolic rate (kcal/day) by the Mifflin–St Jeor equation. */
export function basalMetabolicRate({ sex, ageYears, heightCm, weightKg }: Body): number {
  const base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears
  return sex === 'MALE' ? base + 5 : base - 161
}

export interface ActivityLevel {
  value: string
  label: string
  multiplier: number
}

export const ACTIVITY_LEVELS: ActivityLevel[] = [
  { value: 'sedentary', label: 'Hareketsiz — masa başı, antrenman yok', multiplier: 1.2 },
  { value: 'light', label: 'Hafif aktif — haftada 1–3 gün antrenman', multiplier: 1.375 },
  { value: 'moderate', label: 'Orta aktif — haftada 3–5 gün antrenman', multiplier: 1.55 },
  { value: 'high', label: 'Çok aktif — haftada 6–7 gün antrenman', multiplier: 1.725 },
  { value: 'extreme', label: 'Aşırı aktif — günde iki antrenman ya da ağır fiziksel iş', multiplier: 1.9 },
]

export interface Goal {
  value: string
  label: string
  /** Share of maintenance calories eaten: 0.85 is a 15% deficit. */
  factor: number
}

export const GOALS: Goal[] = [
  { value: 'lose', label: 'Yağ yakmak (yaklaşık %15 açık)', factor: 0.85 },
  { value: 'maintain', label: 'Kiloyu korumak', factor: 1 },
  { value: 'gain', label: 'Kas kazanmak (yaklaşık %10 fazla)', factor: 1.1 },
]

export interface CalorieEstimate {
  bmr: number
  maintenance: number
  target: number
  /** True when the goal would have put the target under the resting rate, so it was held at it. */
  heldAtBmr: boolean
}

/** Maintenance is BMR times the activity factor; the target moves it by the goal and never goes under BMR. */
export function estimateCalories(body: Body, activity: ActivityLevel, goal: Goal): CalorieEstimate {
  const bmr = basalMetabolicRate(body)
  const maintenance = bmr * activity.multiplier
  const wanted = maintenance * goal.factor
  const heldAtBmr = wanted < bmr
  return {
    bmr: Math.round(bmr),
    maintenance: Math.round(maintenance),
    target: Math.round(heldAtBmr ? bmr : wanted),
    heldAtBmr,
  }
}

export interface MacroSplit {
  value: string
  label: string
  carbsPercent: number
  fatPercent: number
  proteinPercent: number
}

export const MACRO_SPLITS: MacroSplit[] = [
  { value: 'balanced', label: 'Dengeli', carbsPercent: 45, fatPercent: 30, proteinPercent: 25 },
  { value: 'high-protein', label: 'Yüksek protein', carbsPercent: 40, fatPercent: 25, proteinPercent: 35 },
  { value: 'low-carb', label: 'Düşük karbonhidrat', carbsPercent: 25, fatPercent: 45, proteinPercent: 30 },
]

export interface BodyMassIndex {
  value: number
  category: string
  /** Weights that fall inside the "normal" band for this height. */
  healthyRangeKg: [number, number]
}

/** World Health Organization bands; they say nothing about muscle, which is why the page says so. */
export function bodyMassIndex(heightCm: number, weightKg: number): BodyMassIndex {
  const meters = heightCm / 100
  const value = weightKg / (meters * meters)
  const category =
    value < 18.5 ? 'Zayıf' : value < 25 ? 'Normal' : value < 30 ? 'Fazla kilolu' : 'Obez'
  return {
    value,
    category,
    healthyRangeKg: [18.5 * meters * meters, 24.9 * meters * meters],
  }
}
