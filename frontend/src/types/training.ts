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

export function muscleGroupLabel(group: MuscleGroup): string {
  return MUSCLE_GROUPS.find((entry) => entry.value === group)?.label ?? group
}

export interface Exercise {
  id: string
  name: string
  muscleGroup: MuscleGroup
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
  muscleGroup: MuscleGroup
  videoUrl?: string | null
  note?: string | null
}

export interface SetLog {
  planExerciseId: string
  setNumber: number
  reps: number
  weightKg: number | null
}

export interface PlanExercise {
  id: string
  exercise: Exercise
  displayOrder: number
  setCount: number
  /** Free text: "8-10", "AMRAP", … */
  reps: string
  targetWeightKg: number | null
  restSeconds: number | null
  note: string | null
  /** What the member recorded last time; empty on the coach side. */
  lastPerformed: SetLog[]
}

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
  exerciseId: string
  setCount: number
  reps: string
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
  sets: { planExerciseId: string; setNumber: number; reps: number; weightKg?: number | null }[]
}
