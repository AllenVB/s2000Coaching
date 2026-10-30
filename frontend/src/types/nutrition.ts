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

/** A plan has one half for training days and one for rest days, each with its own targets and meals. */
export type NutritionDayType = 'TRAINING' | 'REST'

export const DAY_TYPES: { value: NutritionDayType; label: string; short: string }[] = [
  { value: 'TRAINING', label: 'Antrenman Günleri', short: 'Antrenman günü' },
  { value: 'REST', label: 'Dinlenme Günleri', short: 'Dinlenme günü' },
]

export function dayTypeLabel(dayType: NutritionDayType): string {
  return DAY_TYPES.find((entry) => entry.value === dayType)?.short ?? dayType
}

export interface Meal {
  id: string
  displayOrder: number
  name: string
  timeHint: string | null
  note: string | null
  kind: MealKind
  items: MealItem[]
  /** Which half of the plan this belongs to. */
  dayType: NutritionDayType
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
  /** The same four targets for a rest day; the ones above are the training-day targets. */
  restTargetCalories: number | null
  restTargetProteinG: number | null
  restTargetCarbsG: number | null
  restTargetFatG: number | null
  /** Daily water in litres. */
  waterLiters: number | null
  updatedAt: string
  meals: Meal[]
}

export interface DayTargets {
  calories: number | null
  proteinG: number | null
  carbsG: number | null
  fatG: number | null
}

/** The calorie and macro targets of one kind of day. */
export function targetsOf(plan: NutritionPlan, dayType: NutritionDayType): DayTargets {
  return dayType === 'REST'
    ? {
        calories: plan.restTargetCalories,
        proteinG: plan.restTargetProteinG,
        carbsG: plan.restTargetCarbsG,
        fatG: plan.restTargetFatG,
      }
    : {
        calories: plan.targetCalories,
        proteinG: plan.targetProteinG,
        carbsG: plan.targetCarbsG,
        fatG: plan.targetFatG,
      }
}

export function hasTargets(targets: DayTargets): boolean {
  return targets.calories !== null || targets.proteinG !== null || targets.carbsG !== null || targets.fatG !== null
}

/** The meals and supplement blocks of one kind of day. */
export function mealsOfDay(plan: NutritionPlan, dayType: NutritionDayType): Meal[] {
  return plan.meals.filter((meal) => meal.dayType === dayType)
}

export interface UpsertNutritionPlanInput {
  name: string
  note?: string | null
  targetCalories?: number | null
  targetProteinG?: number | null
  targetCarbsG?: number | null
  targetFatG?: number | null
  restTargetCalories?: number | null
  restTargetProteinG?: number | null
  restTargetCarbsG?: number | null
  restTargetFatG?: number | null
  waterLiters?: number | null
}

export interface UpsertMealInput {
  name: string
  timeHint?: string | null
  note?: string | null
  kind?: MealKind
  /** Which half a new meal goes into; training days when left out. */
  dayType?: NutritionDayType
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

/** What one kind of day's listed foods actually add up to — compared against that day's target. */
export function planTotals(plan: NutritionPlan, dayType: NutritionDayType): MacroTotals {
  return mealsOfDay(plan, dayType)
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
export function splitBlocks(
  plan: NutritionPlan,
  dayType: NutritionDayType,
): { meals: Meal[]; supplements: Meal[] } {
  const ordered = mealsOfDay(plan, dayType).sort((a, b) => a.displayOrder - b.displayOrder)
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
