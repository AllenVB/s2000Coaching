export const MUSCLE_GROUPS = [
  { value: 'GOGUS', label: 'Göğüs' },
  { value: 'SIRT', label: 'Sırt' },
  { value: 'OMUZ', label: 'Omuz' },
  { value: 'KOL', label: 'Kol' },
  { value: 'BACAK', label: 'Bacak' },
  { value: 'KARIN', label: 'Karın' },
  { value: 'KARDIYO', label: 'Kardiyo' },
  { value: 'TUM_VUCUT', label: 'Tüm Vücut' },
] as const

export type MuscleGroup = (typeof MUSCLE_GROUPS)[number]['value']

/** Empty for an exercise the coach only typed into a plan, which has no group. */
export function muscleGroupLabel(group: MuscleGroup | null): string {
  return group === null ? '' : (MUSCLE_GROUPS.find((entry) => entry.value === group)?.label ?? group)
}

export interface Exercise {
  id: string
  name: string
  muscleGroup: MuscleGroup | null
  /** The address as stored; for an old entry that is not YouTube it is all there is. */
  videoUrl: string | null
  /** What the player is built from; null when there is no YouTube video. */
  video: ExerciseVideo | null
  note: string | null
}

export interface ExerciseVideo {
  id: string
  startSeconds: number
}

export interface UpsertExerciseInput {
  name: string
  muscleGroup: MuscleGroup | null
  videoUrl?: string | null
  note?: string | null
}

export interface SetLog {
  planExerciseId: string
  setNumber: number
  reps: number
  weightKg: number | null
  rpe: number | null
}

export interface PlanExercise {
  id: string
  exercise: Exercise
  displayOrder: number
  setCount: number
  /** Free text: "8-10", "AMRAP", "30-25-20" — the per-set targets joined when there are some. */
  reps: string
  /**
   * The rep target of each set as the coach wrote them; empty for an exercise written with
   * one free-text reps. A plan cached on a phone before this existed has no such field at all.
   */
  setReps?: string[]
  targetWeightKg: number | null
  restSeconds: number | null
  note: string | null
  /** What the member recorded last time; empty on the coach side. */
  lastPerformed: SetLog[]
  /** Top weight of each of the member's sessions of this exercise, oldest first; at most ten. */
  weightHistory?: WeightEntry[]
}

export interface WeightEntry {
  /** Counts the member's sessions of this exercise from 1, and keeps counting past ten. */
  sessionNumber: number
  performedOn: string
  /** Null when the sets were recorded without a weight. */
  topWeightKg: number | null
}

/** Most sets one exercise can have; the server enforces the same number. */
export const MAX_SETS = 10

/** A programme has between one and seven training days. */
export const MAX_DAYS = 7

export interface PlanDay {
  id: string
  dayOrder: number
  name: string
  note: string | null
  lastPerformedOn: string | null
  exercises: PlanExercise[]
}

export interface TrainingPlan {
  id: string
  name: string
  note: string | null
  template: boolean
  active: boolean
  updatedAt: string
  days: PlanDay[]
}

export interface UpsertPlanInput {
  name: string
  note?: string | null
}

export interface UpsertPlanExerciseInput {
  /** Either an exercise already in the catalogue … */
  exerciseId?: string
  /** … or its name as the coach typed it (found or created in the catalogue). */
  exerciseName?: string
  /** The rep target of each set, up to ten; blank cells after the last filled one are fine, gaps are not. */
  setReps: string[]
  setCount?: number
  reps?: string
  targetWeightKg?: number | null
  restSeconds?: number | null
  note?: string | null
}

export interface WorkoutLog {
  id: string
  dayId: string
  dayName: string
  performedOn: string
  note: string | null
  sets: SetLog[]
}

export interface SaveWorkoutLogInput {
  dayId: string
  performedOn: string
  note?: string | null
  sets: {
    planExerciseId: string
    setNumber: number
    reps: number
    weightKg?: number | null
    /** Perceived effort 1–10; left out when the member did not say. */
    rpe?: number | null
  }[]
}

/** Mirrors the backend's training statistics; every suggestion carries its own reasoning. */
export interface TrainingStats {
  exercises: ExerciseStats[]
  weeklyVolume: WeekVolume[]
}

export interface ExerciseStats {
  exerciseId: string
  name: string
  sessions: number
  lastSessionOn: string
  lastTopWeightKg: number | null
  /** Null when no set qualified: no weight logged, or only sets above 12 reps. */
  best: { estimatedOneRepMaxKg: number; weightKg: number; reps: number; on: string } | null
  newRecordInLastSession: boolean
  suggestion: { kind: 'INCREASE' | 'HOLD' | 'INSUFFICIENT'; nextWeightKg: number | null; text: string }
}

export interface WeekVolume {
  weekStart: string
  sets: number
  volumeKg: number
}
