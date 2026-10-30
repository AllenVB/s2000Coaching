/** Energy and grams; `calories` is kcal, the rest are grams. */
export interface Macros {
  calories: number
  carbsG: number
  fatG: number
  proteinG: number
}

/** One Monday-to-Sunday week of a member's eating, with the grams the server worked out. */
export interface WeeklyNutrition {
  id: string
  /** The Monday, `YYYY-MM-DD`. */
  weekStart: string
  /** The Sunday. */
  weekEnd: string
  calories: number
  carbsPercent: number
  fatPercent: number
  proteinPercent: number
  weekly: Macros
  dailyAverage: Macros
  hasPhoto: boolean
  createdAt: string
  updatedAt: string
}

export interface SaveWeeklyNutritionInput {
  weekStart: string
  calories: number
  carbsPercent: number
  fatPercent: number
  proteinPercent: number
  photo: File | null
  removePhoto: boolean
}
