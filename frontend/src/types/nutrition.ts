export interface MealItem {
  id: string
  displayOrder: number
  name: string
  /** Free text: "80 g", "1 orta boy". */
  amount: string | null
  calories: number | null
  proteinG: number | null
  carbsG: number | null
  fatG: number | null
  note: string | null
}

/** A supplement block is laid out like a meal but is not food, so it is counted apart. */
export type MealKind = 'MEAL' | 'SUPPLEMENT'

export interface Meal {
  id: string
  displayOrder: number
  name: string
  timeHint: string | null
  note: string | null
  kind: MealKind
  items: MealItem[]
}

export interface NutritionPlan {
  id: string
  name: string
  note: string | null
  template: boolean
  active: boolean
  targetCalories: number | null
  targetProteinG: number | null
  targetCarbsG: number | null
  targetFatG: number | null
  /** Daily water in litres. */
  waterLiters: number | null
  updatedAt: string
  meals: Meal[]
}

export interface UpsertNutritionPlanInput {
  name: string
  note?: string | null
  targetCalories?: number | null
  targetProteinG?: number | null
  targetCarbsG?: number | null
  targetFatG?: number | null
  waterLiters?: number | null
}

export interface UpsertMealInput {
  name: string
  timeHint?: string | null
  note?: string | null
  kind?: MealKind
}

export interface UpsertMealItemInput {
  name: string
  amount?: string | null
  calories?: number | null
  proteinG?: number | null
  carbsG?: number | null
  fatG?: number | null
  note?: string | null
}

export interface MacroTotals {
  calories: number
  proteinG: number
  carbsG: number
  fatG: number
}

/** What the listed foods actually add up to — compared against the plan's target. */
export function planTotals(plan: NutritionPlan): MacroTotals {
  return plan.meals
    .flatMap((meal) => meal.items)
    .reduce<MacroTotals>(
      (total, item) => ({
        calories: total.calories + (item.calories ?? 0),
        proteinG: total.proteinG + (item.proteinG ?? 0),
        carbsG: total.carbsG + (item.carbsG ?? 0),
        fatG: total.fatG + (item.fatG ?? 0),
      }),
      { calories: 0, proteinG: 0, carbsG: 0, fatG: 0 },
    )
}

export function mealCalories(meal: Meal): number {
  return meal.items.reduce((total, item) => total + (item.calories ?? 0), 0)
}

/** Meals and supplement blocks, each in the order the coach set them. */
export function splitBlocks(plan: NutritionPlan): { meals: Meal[]; supplements: Meal[] } {
  const ordered = [...plan.meals].sort((a, b) => a.displayOrder - b.displayOrder)
  return {
    meals: ordered.filter((meal) => meal.kind !== 'SUPPLEMENT'),
    supplements: ordered.filter((meal) => meal.kind === 'SUPPLEMENT'),
  }
}

/** Foods and supplements still waiting for an amount — what is left for the coach to fill in. */
export function missingAmounts(plan: NutritionPlan): number {
  return plan.meals
    .flatMap((meal) => meal.items)
    .filter((item) => item.amount === null || item.amount.trim() === '').length
}
