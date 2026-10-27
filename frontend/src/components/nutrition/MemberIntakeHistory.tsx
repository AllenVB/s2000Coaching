import { useMemberIntake } from '@/api/intakeApi'
import { formatDate } from '@/lib/date'

/** The coach's read of a member's food log. Read-only: only the member writes their own days. */
export function MemberIntakeHistory({ memberId, targetCalories }: { memberId: string; targetCalories: number | null }) {
  const { data: entries, isLoading, isError, error } = useMemberIntake(memberId)

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Yemek Kayıtları</h2>
      {isLoading ? (
        <div className="h-16 animate-pulse rounded-xl bg-surface-elevated" />
      ) : isError ? (
        <p className="text-xs text-error">Yüklenemedi: {error.message}</p>
      ) : !entries || entries.length === 0 ? (
        <p className="text-sm italic text-text-muted">Üye henüz yemek kaydı girmedi.</p>
      ) : (
        <>
          <p className="mb-3 text-xs text-text-muted">
            Son {Math.min(entries.length, 14)} gün
            {targetCalories ? ` · hedef ${targetCalories} kcal` : ''}
          </p>
          <ul className="space-y-1.5">
            {entries.slice(0, 14).map((entry) => (
              <li
                key={entry.id}
                className="flex flex-wrap items-baseline justify-between gap-3 rounded-lg border border-border bg-surface-elevated px-3 py-2 text-xs"
              >
                <span className="font-semibold text-text-primary">{formatDate(entry.consumedOn)}</span>
                <span className="text-text-muted">
                  {entry.proteinG !== null ? `P ${entry.proteinG} ` : ''}
                  {entry.carbsG !== null ? `K ${entry.carbsG} ` : ''}
                  {entry.fatG !== null ? `Y ${entry.fatG} ` : ''}
                  {entry.sodiumMg !== null ? `Na ${entry.sodiumMg}` : ''}
                </span>
                <span className="font-semibold text-text-primary">{entry.calories} kcal</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
