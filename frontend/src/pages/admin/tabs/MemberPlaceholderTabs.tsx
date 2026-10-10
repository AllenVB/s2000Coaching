import { ComingSoonCard } from '@/components/ui/PageHeading'

export function MemberTrainingTab() {
  return (
    <ComingSoonCard
      title="Antrenman Planı"
      description="Hareket katalogu, plan şablonları ve üyeye atanan program bu sekmede yönetilecek."
    />
  )
}

export function MemberNutritionTab() {
  return (
    <ComingSoonCard
      title="Beslenme Planı"
      description="Makro hedefleri ve öğün planı bu sekmede düzenlenecek."
    />
  )
}
