import { mealsOfDay, type NutritionDayType, type NutritionPlan } from '@/types/nutrition'

export interface MacroTotals {
  calories: number
  proteinG: number
  carbsG: number
  fatG: number
}

/**
 * What the plan's meals actually add up to. Items are allowed to leave macros blank — a
 * coach may only fill in calories — so a missing value counts as zero rather than voiding
 * the whole total.
 */
export function plannedTotals(plan: NutritionPlan, dayType: NutritionDayType): MacroTotals {
  const totals: MacroTotals = { calories: 0, proteinG: 0, carbsG: 0, fatG: 0 }

  for (const meal of mealsOfDay(plan, dayType)) {
    for (const item of meal.items) {
      totals.calories += item.calories ?? 0
      totals.proteinG += item.proteinG ?? 0
      totals.carbsG += item.carbsG ?? 0
      totals.fatG += item.fatG ?? 0
    }
  }
  return totals
}

export function hasAnyMacro(totals: MacroTotals): boolean {
  return totals.calories > 0 || totals.proteinG > 0 || totals.carbsG > 0 || totals.fatG > 0
}
