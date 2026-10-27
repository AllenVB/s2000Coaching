export type QuestionnaireGoal = 'YAG_YAKMA' | 'KAS_KAZANMA' | 'FORMDA_KALMA' | 'PERFORMANS'
export type TrainingEquipment = 'SALON' | 'EVDE' | 'KARISIK'

export const GOALS: { value: QuestionnaireGoal; label: string }[] = [
  { value: 'YAG_YAKMA', label: 'Yağ yakmak' },
  { value: 'KAS_KAZANMA', label: 'Kas kazanmak' },
  { value: 'FORMDA_KALMA', label: 'Formumu korumak' },
  { value: 'PERFORMANS', label: 'Performans / güç' },
]

export const EQUIPMENT: { value: TrainingEquipment; label: string }[] = [
  { value: 'SALON', label: 'Spor salonu' },
  { value: 'EVDE', label: 'Evde' },
  { value: 'KARISIK', label: 'Karışık' },
]

export interface Questionnaire {
  /** The PAR-Q wording, sent by the server so it lives in one place. */
  questions: string[]
  /** Null until the member has filled the questionnaire in. */
  answers: {
    parq: boolean[]
    /** True when any readiness question was answered yes — ask for a doctor's clearance. */
    needsMedicalClearance: boolean
    goal: QuestionnaireGoal
    trainingDaysPerWeek: number
    equipment: TrainingEquipment
    injuries: string | null
    completedAt: string
  } | null
}

export interface UpsertQuestionnaireInput {
  parq: boolean[]
  goal: QuestionnaireGoal
  trainingDaysPerWeek: number
  equipment: TrainingEquipment
  injuries?: string | null
}
