import { useState } from 'react'

import { useDeleteMyIntake, useMyIntake, useSaveMyIntake } from '@/api/intakeApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import type { NutritionPlan } from '@/types/nutrition'

/** How many recent days the list shows; older ones are still stored and analysed. */
const SHOWN_DAYS = 14

const today = () => new Date().toISOString().slice(0, 10)

/** Blank means "not recorded" (null), never zero — a zero would read as "ate nothing". */
const optionalNumber = (value: string): number | null => (value.trim() === '' ? null : Number(value))

/**
 * The member's daily food log: one entry per day with the day's totals. Totals rather than
 * items on purpose — the analysis works on days, and a member who has to weigh every gram
 * stops logging within a week.
 */
export function IntakeLog({ plan }: { plan: NutritionPlan | null }) {
  const { data: entries, isLoading } = useMyIntake()
  const saveIntake = useSaveMyIntake()
  const deleteIntake = useDeleteMyIntake()

  const [consumedOn, setConsumedOn] = useState(today)
  const [calories, setCalories] = useState('')
  const [protein, setProtein] = useState('')
  const [carbs, setCarbs] = useState('')
  const [fat, setFat] = useState('')
  const [sodium, setSodium] = useState('')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (calories.trim() === '') {
      return
    }
    saveIntake.mutate(
      {
        consumedOn,
        calories: Number(calories),
        proteinG: optionalNumber(protein),
        carbsG: optionalNumber(carbs),
        fatG: optionalNumber(fat),
        sodiumMg: optionalNumber(sodium),
      },
      {
        onSuccess: () => {
          setCalories('')
          setProtein('')
          setCarbs('')
          setFat('')
          setSodium('')
        },
      },
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">Yemek Kaydı</h2>
      <p className="mb-5 text-xs leading-relaxed text-text-secondary">
        Günün toplamını girin. Aynı güne tekrar girerseniz o günün kaydı güncellenir. Protein, karbonhidrat,
        yağ ve sodyum isteğe bağlıdır; boş bırakılan alan "girilmedi" sayılır.
      </p>

      <form className="space-y-4" onSubmit={submit}>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
          <Field label="Tarih">
            <input
              type="date"
              value={consumedOn}
              max={today()}
              onChange={(event) => setConsumedOn(event.target.value)}
              className={inputClasses}
            />
          </Field>
          <Field label="Kalori (kcal) *">
            <input
              type="number"
              inputMode="numeric"
              min={0}
              max={20000}
              value={calories}
              onChange={(event) => setCalories(event.target.value)}
              className={inputClasses}
            />
          </Field>
        </div>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Field label="Protein (g)">
            <input type="number" inputMode="numeric" min={0} value={protein}
              onChange={(event) => setProtein(event.target.value)} className={inputClasses} />
          </Field>
          <Field label="Karbonhidrat (g)">
            <input type="number" inputMode="numeric" min={0} value={carbs}
              onChange={(event) => setCarbs(event.target.value)} className={inputClasses} />
          </Field>
          <Field label="Yağ (g)">
            <input type="number" inputMode="numeric" min={0} value={fat}
              onChange={(event) => setFat(event.target.value)} className={inputClasses} />
          </Field>
          <Field label="Sodyum (mg)">
            <input type="number" inputMode="numeric" min={0} value={sodium}
              onChange={(event) => setSodium(event.target.value)} className={inputClasses} />
          </Field>
        </div>

        {saveIntake.isError ? <p className="text-xs text-error">{saveIntake.error.message}</p> : null}

        <Button type="submit" disabled={saveIntake.isPending || calories.trim() === ''}>
          {saveIntake.isPending ? 'Kaydediliyor…' : 'Günü Kaydet'}
        </Button>
      </form>

      <div className="mt-6 border-t border-border pt-4">
        {isLoading ? (
          <div className="h-16 animate-pulse rounded-xl bg-surface-elevated" />
        ) : !entries || entries.length === 0 ? (
          <p className="text-sm italic text-text-muted">Henüz yemek kaydı girmediniz.</p>
        ) : (
          <ul className="space-y-2">
            {entries.slice(0, SHOWN_DAYS).map((entry) => (
              <li
                key={entry.id}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3"
              >
                <span className="min-w-0">
                  <span className="block text-sm font-semibold text-text-primary">
                    {formatDate(entry.consumedOn)}
                  </span>
                  <span className="block text-xs text-text-muted">{macroLine(entry)}</span>
                </span>
                <span className="flex items-center gap-3">
                  <span className="text-sm font-semibold text-text-primary">{entry.calories} kcal</span>
                  <TargetBadge calories={entry.calories} target={plan?.targetCalories ?? null} />
                  <button
                    type="button"
                    onClick={() => deleteIntake.mutate(entry.id)}
                    aria-label={`${formatDate(entry.consumedOn)} kaydını sil`}
                    className="text-text-muted transition-colors hover:text-error"
                  >
                    <TrashIcon className="h-4 w-4" />
                  </button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  )
}

function macroLine(entry: {
  proteinG: number | null
  carbsG: number | null
  fatG: number | null
  sodiumMg: number | null
}): string {
  const parts = [
    entry.proteinG !== null ? `P ${entry.proteinG} g` : null,
    entry.carbsG !== null ? `K ${entry.carbsG} g` : null,
    entry.fatG !== null ? `Y ${entry.fatG} g` : null,
    entry.sodiumMg !== null ? `Na ${entry.sodiumMg} mg` : null,
  ].filter((part): part is string => part !== null)
  return parts.length > 0 ? parts.join(' · ') : 'Makro girilmedi'
}

/** Within 10% of the target counts as on target; the badge says how far off otherwise. */
function TargetBadge({ calories, target }: { calories: number; target: number | null }) {
  if (!target || target <= 0) {
    return null
  }
  const diff = calories - target
  const onTarget = Math.abs(diff) <= target * 0.1
  return (
    <span
      className={`rounded-md px-2 py-0.5 text-[11px] font-semibold ${
        onTarget ? 'bg-success/15 text-success' : 'bg-accent-orange/15 text-accent-orange'
      }`}
    >
      {onTarget ? 'hedefte' : `${diff > 0 ? '+' : ''}${diff} kcal`}
    </span>
  )
}
