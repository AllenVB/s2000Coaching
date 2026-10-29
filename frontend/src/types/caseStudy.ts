import type { PhotoPose } from '@/types/member'

export type Sex = 'ERKEK' | 'KADIN'

/**
 * Mirrors the backend's `BodyAnalysis`. Every block carries the window it covers and the
 * number of observations behind it, and a block that lacked the data to answer returns
 * nulls plus a reason — so the page must render the reason, never an empty space where a
 * number was expected.
 */
export interface BodyAnalysis {
  coverage: Coverage
  dailySeries: DailyPoint[]
  weeklyRows: WeeklyRow[]
  lagFinding: LagFinding
  tdeeWindows: TdeeWindow[]
  calorieBuckets: CalorieBucket[]
  sodiumFinding: SodiumFinding
  longTermPeriods: Period[]
  bodyComposition: BodyComposition
  caveats: string[]
}

export interface Coverage {
  measurementFirst: string | null
  measurementLast: string | null
  measurementDays: number
  intakeFirst: string | null
  intakeLast: string | null
  intakeDays: number
  intakeMissingDays: number
  sodiumDays: number
  hasNeck: boolean
  hasWaist: boolean
}

export interface DailyPoint {
  date: string
  weightKg: number | null
  trendWeightKg: number | null
  waistCm: number | null
  calories: number | null
}

export interface WeeklyRow {
  weekStart: string
  intakeDaysLogged: number
  avgCalories: number | null
  avgProteinG: number | null
  avgCarbsG: number | null
  avgFatG: number | null
  avgSodiumMg: number | null
  trendWeightEnd: number | null
  trendWeightDelta: number | null
  waistDelta: number | null
}

export interface LagFinding {
  sampleSize: number | null
  correlationSameWeek: number | null
  correlationNextWeek: number | null
  betterFit: 'AYNI_HAFTA' | 'SONRAKI_HAFTA' | null
  note: string
}

export interface TdeeWindow {
  windowStart: string
  windowEnd: string
  days: number
  avgCalories: number
  trendWeightChangeKg: number
  estimatedTdee: number
}

export interface CalorieBucket {
  label: string
  minCalories: number
  maxCalories: number | null
  weekCount: number
  avgWeeklyWeightDelta: number | null
  avgWeeklyWaistDelta: number | null
  gainedWithoutWaist: boolean
}

export interface SodiumFinding {
  sampleSize: number | null
  correlationWithNextDayWeight: number | null
  note: string
}

export interface Period {
  start: string
  end: string
  label: 'KILO_ALMA' | 'KILO_VERME' | 'SABIT'
  weightChangeKg: number
  waistChangeCm: number | null
  waistPerKg: number | null
}

export interface BodyComposition {
  method: string | null
  bodyFatPercent: number | null
  leanMassKg: number | null
  fatMassKg: number | null
  uncertaintyPoints: number
  asOf: string | null
  note: string
}

export interface PublicCaseStudySummary {
  slug: string
  title: string
  summary: string | null
  firstDate: string | null
  lastDate: string | null
  measurementDays: number
  intakeDays: number
}

export interface PublicCaseStudy {
  slug: string
  title: string
  summary: string | null
  heightCm: number | null
  sex: Sex | null
  updatedAt: string
  analysis: BodyAnalysis
  /** Null means the coach has not switched photos on; an empty list means shown, none matched. */
  photoPeriods: PeriodPhotos[] | null
}

export interface CaseStudy {
  id: string
  slug: string
  title: string
  summary: string | null
  heightCm: number | null
  sex: Sex | null
  birthYear: number | null
  published: boolean
  photosPublished: boolean
  measurementCount: number
  intakeCount: number
  photoCount: number
  createdAt: string
  updatedAt: string
}

export interface UpsertCaseStudyInput {
  title: string
  summary: string | null
  heightCm: number | null
  sex: Sex | null
  birthYear: number | null
}

export type DataKind = 'MEASUREMENTS' | 'INTAKE'

export interface CsvPreview {
  headers: string[]
  /** Canonical field name to column index; a field missing here matched nothing. */
  guessedMapping: Record<string, number>
  availableFields: string[]
  totalRows: number
  sampleRows: string[][]
}

export interface ImportResult {
  importedRows: number
  updatedRows: number
  skippedRows: number
  firstDate: string | null
  lastDate: string | null
  warnings: string[]
}

/** Turkish labels for the canonical field names the import understands. */
export const FIELD_LABELS: Record<string, string> = {
  measuredOn: 'Ölçüm tarihi',
  weightKg: 'Kilo (kg)',
  neckCm: 'Boyun (cm)',
  waistCm: 'Bel (cm)',
  chestCm: 'Göğüs (cm)',
  hipCm: 'Kalça (cm)',
  armCm: 'Kol (cm)',
  thighCm: 'Bacak (cm)',
  shoulderCm: 'Omuz (cm)',
  bodyFatPercent: 'Yağ oranı (%)',
  consumedOn: 'Kayıt tarihi',
  calories: 'Kalori (kcal)',
  proteinG: 'Protein (g)',
  carbsG: 'Karbonhidrat (g)',
  fatG: 'Yağ (g)',
  sodiumMg: 'Sodyum (mg)',
}

/** Fields an import cannot proceed without. */
export const REQUIRED_FIELDS: Record<DataKind, string[]> = {
  MEASUREMENTS: ['measuredOn', 'weightKg'],
  INTAKE: ['consumedOn', 'calories'],
}

export const PERIOD_LABELS: Record<Period['label'], string> = {
  KILO_ALMA: 'Kilo alma',
  KILO_VERME: 'Kilo verme',
  SABIT: 'Sabit',
}

export interface CaseStudyPhoto {
  id: string
  takenOn: string
  pose: PhotoPose
  sizeBytes: number
  createdAt: string
}

/** Photo date minus boundary date: negative is before the boundary, positive after. */
export interface PhotoMatch {
  photoId: string
  takenOn: string
  daysFromTarget: number
}

/** One pose within one period; a missing side comes with the reason in `note`. */
export interface PosePair {
  pose: PhotoPose
  start: PhotoMatch | null
  end: PhotoMatch | null
  note: string | null
}

export interface PeriodPhotos {
  start: string
  end: string
  label: Period['label']
  poses: PosePair[]
  note: string | null
}
