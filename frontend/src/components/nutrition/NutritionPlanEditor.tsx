import { useState } from 'react'

import { useAddMeal, useAddMealItem, useDeleteMeal, useDeleteMealItem } from '@/api/nutritionApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { PlusIcon, TrashIcon } from '@/components/ui/icons'
import { mealCalories, type Meal, type NutritionPlan } from '@/types/nutrition'

/** Meal-and-item editor, shared by the template page and a member's nutrition tab. */
export function NutritionPlanEditor({ plan }: { plan: NutritionPlan }) {
  const addMeal = useAddMeal()
  const [name, setName] = useState('')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed.length > 0) {
      addMeal.mutate({ planId: plan.id, input: { name: trimmed } }, { onSuccess: () => setName('') })
    }
  }

  return (
    <div className="space-y-4">
      {plan.meals.map((meal) => (
        <MealCard key={meal.id} meal={meal} />
      ))}

      <form className="flex flex-wrap items-center gap-2" onSubmit={submit}>
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          type="text"
          placeholder="Örn: Kahvaltı"
          aria-label="Yeni öğün adı"
          className={`${inputClasses} max-w-xs`}
        />
        <Button type="submit" variant="secondary" disabled={addMeal.isPending || name.trim().length === 0}>
          <PlusIcon className="h-3.5 w-3.5" />
          Öğün Ekle
        </Button>
      </form>

      {addMeal.isError ? <p className="text-xs text-error">{addMeal.error.message}</p> : null}
    </div>
  )
}

function MealCard({ meal }: { meal: Meal }) {
  const deleteMeal = useDeleteMeal()
  const deleteItem = useDeleteMealItem()
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)
  const [isAdding, setAdding] = useState(false)

  return (
    <section className="rounded-2xl border border-border bg-surface-elevated p-5">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wider text-text-primary">{meal.name}</h3>
          <p className="mt-0.5 text-xs text-text-muted">
            {meal.timeHint ? `${meal.timeHint} · ` : ''}
            {mealCalories(meal)} kcal
          </p>
        </div>
        <button
          type="button"
          onClick={() => setConfirmingDelete(true)}
          aria-label={`${meal.name} öğününü sil`}
          className="text-text-muted transition-colors hover:text-error"
        >
          <TrashIcon className="h-4 w-4" />
        </button>
      </div>

      {meal.items.length === 0 ? (
        <p className="text-sm italic text-text-muted">Bu öğüne henüz besin eklenmedi.</p>
      ) : (
        <ul className="space-y-2">
          {meal.items.map((item) => (
            <li
              key={item.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface px-4 py-3"
            >
              <span className="min-w-0">
                <span className="block font-semibold text-text-primary">
                  {item.name}
                  {item.amount ? <span className="font-normal text-text-secondary"> · {item.amount}</span> : null}
                </span>
                <span className="block text-xs text-text-muted">{macroLine(item)}</span>
              </span>
              <button
                type="button"
                onClick={() => deleteItem.mutate({ itemId: item.id })}
                aria-label={`${item.name} besinini çıkar`}
                className="text-text-muted transition-colors hover:text-error"
              >
                <TrashIcon className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}

      {isAdding ? (
        <AddItemForm mealId={meal.id} onDone={() => setAdding(false)} />
      ) : (
        <Button type="button" variant="ghost" size="sm" className="mt-3" onClick={() => setAdding(true)}>
          <PlusIcon className="h-3.5 w-3.5" />
          Besin Ekle
        </Button>
      )}

      {isConfirmingDelete ? (
        <ConfirmDialog
          title="Öğünü Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteMeal.isPending}
          error={deleteMeal.isError ? deleteMeal.error.message : null}
          description={`"${meal.name}" öğününü ve içindeki besinleri silmek istediğinize emin misiniz?`}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() =>
            deleteMeal.mutate({ mealId: meal.id }, { onSuccess: () => setConfirmingDelete(false) })
          }
        />
      ) : null}
    </section>
  )
}

function AddItemForm({ mealId, onDone }: { mealId: string; onDone: () => void }) {
  const addItem = useAddMealItem()

  const [name, setName] = useState('')
  const [amount, setAmount] = useState('')
  const [calories, setCalories] = useState('')
  const [protein, setProtein] = useState('')
  const [carbs, setCarbs] = useState('')
  const [fat, setFat] = useState('')

  const numberOrNull = (value: string) => (value.trim() === '' ? null : Number(value))

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed.length === 0) {
      return
    }
    addItem.mutate(
      {
        mealId,
        input: {
          name: trimmed,
          amount: amount.trim() || null,
          calories: numberOrNull(calories),
          proteinG: numberOrNull(protein),
          carbsG: numberOrNull(carbs),
          fatG: numberOrNull(fat),
        },
      },
      { onSuccess: onDone },
    )
  }

  return (
    <form className="mt-4 space-y-3 rounded-xl border border-border bg-surface p-4" onSubmit={submit}>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-6">
        <label className="col-span-2">
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Besin</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            type="text"
            placeholder="Örn: Yulaf"
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Miktar</span>
          <input
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
            type="text"
            placeholder="80 g"
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">kcal</span>
          <input
            value={calories}
            onChange={(event) => setCalories(event.target.value)}
            type="number"
            min={0}
            placeholder="—"
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">P (g)</span>
          <input
            value={protein}
            onChange={(event) => setProtein(event.target.value)}
            type="number"
            min={0}
            placeholder="—"
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">K (g)</span>
          <input
            value={carbs}
            onChange={(event) => setCarbs(event.target.value)}
            type="number"
            min={0}
            placeholder="—"
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Y (g)</span>
          <input
            value={fat}
            onChange={(event) => setFat(event.target.value)}
            type="number"
            min={0}
            placeholder="—"
            className={inputClasses}
          />
        </label>
      </div>

      {addItem.isError ? <p className="text-xs text-error">{addItem.error.message}</p> : null}

      <div className="flex flex-wrap gap-2">
        <Button type="submit" size="sm" disabled={addItem.isPending || name.trim().length === 0}>
          {addItem.isPending ? 'Ekleniyor…' : 'Ekle'}
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onDone}>
          Vazgeç
        </Button>
      </div>
    </form>
  )
}

function macroLine(item: { calories: number | null; proteinG: number | null; carbsG: number | null; fatG: number | null }): string {
  const parts: string[] = []
  if (item.calories !== null) parts.push(`${item.calories} kcal`)
  if (item.proteinG !== null) parts.push(`P ${item.proteinG}g`)
  if (item.carbsG !== null) parts.push(`K ${item.carbsG}g`)
  if (item.fatG !== null) parts.push(`Y ${item.fatG}g`)
  return parts.length > 0 ? parts.join(' · ') : 'makro girilmemiş'
}
