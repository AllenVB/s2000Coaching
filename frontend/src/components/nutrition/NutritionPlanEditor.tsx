import { useRef, useState } from 'react'

import {
  useAddMeal,
  useAddMealItem,
  useDeleteMeal,
  useDeleteMealItem,
  useUpdateMeal,
  useUpdateMealItem,
} from '@/api/nutritionApi'
import { PlanTargetsForm } from '@/components/nutrition/PlanTargetsForm'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { PlusIcon, TrashIcon } from '@/components/ui/icons'
import {
  missingAmounts,
  splitBlocks,
  type Meal,
  type MealItem,
  type MealKind,
  type NutritionPlan,
} from '@/types/nutrition'

// Cells look like plain text until pointed at, so a filled plan reads like the printed one.
const cellClasses =
  'h-9 w-full rounded-md border border-transparent bg-transparent px-2 text-sm text-text-primary transition-colors hover:border-border focus:border-primary focus:bg-surface focus:outline-none'
// An empty amount is what the coach still has to write, so it stands out.
const emptyAmountClasses = 'border-dashed !border-primary/50 bg-primary/5 placeholder:text-primary/70'

/**
 * The plan as one editable table per meal or supplement block, shared by the template page
 * and a member's nutrition tab. Every cell saves when it loses focus; the "+" at the foot of a
 * table adds a row and the trash icon removes one. Enter in an amount cell jumps to the next
 * empty-or-not amount below, so the grams can be filled in straight down the page.
 */
export function NutritionPlanEditor({ plan }: { plan: NutritionPlan }) {
  const { meals, supplements } = splitBlocks(plan)
  const missing = missingAmounts(plan)

  return (
    <div className="space-y-6" data-nutrition-editor>
      <PlanTargetsForm key={plan.id} plan={plan} />

      <p className={`text-xs ${missing > 0 ? 'text-primary' : 'text-success'}`}>
        {missing > 0
          ? `${missing} kalemin miktarı henüz boş — turuncu kesikli kutuları doldurun.`
          : 'Tüm miktarlar dolu.'}
      </p>

      <BlockSection title="Beslenme Programı" kind="MEAL" blocks={meals} planId={plan.id} />
      <BlockSection title="Supplement Planı" kind="SUPPLEMENT" blocks={supplements} planId={plan.id} />
    </div>
  )
}

function BlockSection({
  title,
  kind,
  blocks,
  planId,
}: {
  title: string
  kind: MealKind
  blocks: Meal[]
  planId: string
}) {
  const addMeal = useAddMeal()
  const [name, setName] = useState('')
  const noun = kind === 'SUPPLEMENT' ? 'Supplement Bloğu' : 'Öğün'

  const add = () => {
    const trimmed = name.trim()
    if (trimmed.length > 0 && !addMeal.isPending) {
      addMeal.mutate({ planId, input: { name: trimmed, kind } }, { onSuccess: () => setName('') })
    }
  }

  return (
    <section className="space-y-4">
      <h3 className="text-sm font-bold uppercase tracking-wider text-text-primary">{title}</h3>

      {blocks.map((block) => (
        <BlockTable key={block.id} block={block} />
      ))}

      <div className="flex flex-wrap items-center gap-2">
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              add()
            }
          }}
          type="text"
          placeholder={kind === 'SUPPLEMENT' ? 'Örn: Uyumadan önce' : 'Örn: Ara öğün'}
          aria-label={`Yeni ${noun.toLowerCase()} adı`}
          className="h-9 w-full max-w-xs rounded-lg border border-border bg-surface px-3 text-sm text-text-primary focus:border-primary focus:outline-none"
        />
        <button
          type="button"
          onClick={add}
          disabled={addMeal.isPending || name.trim().length === 0}
          aria-label={`${noun} ekle`}
          className="inline-flex h-9 items-center gap-1.5 rounded-full border border-border-strong bg-surface-elevated px-3.5 text-xs font-semibold uppercase tracking-wide text-text-primary transition-colors hover:border-primary/60 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <PlusIcon className="h-3.5 w-3.5" />
          {noun} Ekle
        </button>
      </div>
      {addMeal.isError ? <p className="text-xs text-error">{addMeal.error.message}</p> : null}
    </section>
  )
}

function BlockTable({ block }: { block: Meal }) {
  const updateMeal = useUpdateMeal()
  const deleteMeal = useDeleteMeal()
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)

  const rename = (value: string, input: HTMLInputElement) => {
    const trimmed = value.trim()
    if (trimmed.length === 0) {
      input.value = block.name
      return
    }
    if (trimmed !== block.name) {
      updateMeal.mutate({
        mealId: block.id,
        input: { name: trimmed, timeHint: block.timeHint, note: block.note, kind: block.kind },
      })
    }
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-surface">
      <div className="flex items-center justify-between gap-3 border-b border-border bg-surface-elevated px-3 py-1.5">
        <input
          defaultValue={block.name}
          onBlur={(event) => rename(event.target.value, event.target)}
          onKeyDown={blurOnEnter}
          aria-label={`${block.name} adı`}
          className="h-9 min-w-0 flex-1 rounded-md border border-transparent bg-transparent px-2 text-xs font-bold uppercase tracking-wider text-primary hover:border-border focus:border-primary focus:bg-surface focus:outline-none"
        />
        <button
          type="button"
          onClick={() => setConfirmingDelete(true)}
          aria-label={`${block.name} bloğunu sil`}
          className="text-text-muted transition-colors hover:text-error"
        >
          <TrashIcon className="h-4 w-4" />
        </button>
      </div>

      <table className="w-full border-collapse">
        <thead className="sr-only">
          <tr>
            <th scope="col">Besin</th>
            <th scope="col">Miktar</th>
            <th scope="col">İşlem</th>
          </tr>
        </thead>
        <tbody>
          {block.items.map((item) => (
            <ItemRow key={item.id} item={item} />
          ))}
          <AddItemRow mealId={block.id} />
        </tbody>
      </table>
      {updateMeal.isError ? <p className="px-4 pb-3 text-xs text-error">{updateMeal.error.message}</p> : null}

      {isConfirmingDelete ? (
        <ConfirmDialog
          title="Bloğu Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteMeal.isPending}
          error={deleteMeal.isError ? deleteMeal.error.message : null}
          description={`"${block.name}" bloğunu ve içindeki kalemleri silmek istediğinize emin misiniz?`}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() =>
            deleteMeal.mutate({ mealId: block.id }, { onSuccess: () => setConfirmingDelete(false) })
          }
        />
      ) : null}
    </div>
  )
}

function ItemRow({ item }: { item: MealItem }) {
  const update = useUpdateMealItem()
  const remove = useDeleteMealItem()
  const nameRef = useRef<HTMLInputElement>(null)
  const amountRef = useRef<HTMLInputElement>(null)

  // Both cells are read from the page every time, never from `item`: the server replaces the
  // whole row, so a save that carried a stale sibling value would quietly undo the other edit.
  const save = () => {
    const name = nameRef.current?.value.trim() ?? ''
    const amount = amountRef.current?.value.trim() ?? ''
    if (name.length === 0) {
      if (nameRef.current) nameRef.current.value = item.name
      return
    }
    if (name === item.name && amount === (item.amount ?? '')) {
      return
    }
    update.mutate({
      itemId: item.id,
      input: {
        name,
        amount: amount || null,
        calories: item.calories,
        proteinG: item.proteinG,
        carbsG: item.carbsG,
        fatG: item.fatG,
        note: item.note,
      },
    })
  }

  const isEmpty = (item.amount ?? '').trim() === ''

  return (
    <>
      <tr className="border-t border-border/60 first:border-t-0">
        <td className="w-3/5 px-2 py-1">
          <input
            ref={nameRef}
            defaultValue={item.name}
            onBlur={save}
            onKeyDown={blurOnEnter}
            maxLength={160}
            aria-label={`${item.name} adı`}
            className={cellClasses}
          />
        </td>
        <td className="px-2 py-1">
          <input
            ref={amountRef}
            defaultValue={item.amount ?? ''}
            onBlur={save}
            onKeyDown={jumpToNextAmount}
            maxLength={60}
            placeholder="gram?"
            data-amount
            aria-label={`${item.name} miktarı`}
            className={`${cellClasses} ${isEmpty ? emptyAmountClasses : ''}`}
          />
        </td>
        <td className="w-10 px-2 py-1 text-right">
          <button
            type="button"
            onClick={() => remove.mutate({ itemId: item.id })}
            disabled={remove.isPending}
            aria-label={`${item.name} kalemini çıkar`}
            className="text-text-muted transition-colors hover:text-error disabled:opacity-50"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </td>
      </tr>
      {update.isError || remove.isError ? (
        <tr>
          <td colSpan={3} className="px-4 pb-2 text-xs text-error">
            {(update.error ?? remove.error)?.message}
          </td>
        </tr>
      ) : null}
    </>
  )
}

/** The "+" row at the foot of a table: a name, an optional amount, Enter or "+" to add. */
function AddItemRow({ mealId }: { mealId: string }) {
  const addItem = useAddMealItem()
  const nameRef = useRef<HTMLInputElement>(null)
  const amountRef = useRef<HTMLInputElement>(null)

  const add = () => {
    const name = nameRef.current?.value.trim() ?? ''
    if (name.length === 0 || addItem.isPending) {
      nameRef.current?.focus()
      return
    }
    addItem.mutate(
      {
        mealId,
        input: { name, amount: amountRef.current?.value.trim() || null },
      },
      {
        onSuccess: () => {
          if (nameRef.current) nameRef.current.value = ''
          if (amountRef.current) amountRef.current.value = ''
          nameRef.current?.focus()
        },
      },
    )
  }

  const addOnEnter = (event: React.KeyboardEvent) => {
    if (event.key === 'Enter') {
      event.preventDefault()
      add()
    }
  }

  return (
    <>
      <tr className="border-t border-dashed border-border bg-surface-elevated/40">
        <td className="w-3/5 px-2 py-1">
          <input
            ref={nameRef}
            onKeyDown={addOnEnter}
            maxLength={160}
            placeholder="Yeni besin…"
            aria-label="Yeni besin adı"
            className={cellClasses}
          />
        </td>
        <td className="px-2 py-1">
          <input
            ref={amountRef}
            onKeyDown={addOnEnter}
            maxLength={60}
            placeholder="miktar"
            aria-label="Yeni besin miktarı"
            className={cellClasses}
          />
        </td>
        <td className="w-10 px-2 py-1 text-right">
          <button
            type="button"
            onClick={add}
            disabled={addItem.isPending}
            aria-label="Besin ekle"
            className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-primary text-canvas transition-colors hover:bg-primary-hover disabled:opacity-50"
          >
            <PlusIcon className="h-4 w-4" />
          </button>
        </td>
      </tr>
      {addItem.isError ? (
        <tr>
          <td colSpan={3} className="px-4 pb-2 text-xs text-error">
            {addItem.error.message}
          </td>
        </tr>
      ) : null}
    </>
  )
}

function blurOnEnter(event: React.KeyboardEvent<HTMLInputElement>) {
  if (event.key === 'Enter') {
    event.preventDefault()
    event.currentTarget.blur()
  }
}

/** Enter moves down to the next amount cell (which saves this one on blur); on the last, it just saves. */
function jumpToNextAmount(event: React.KeyboardEvent<HTMLInputElement>) {
  if (event.key !== 'Enter') {
    return
  }
  event.preventDefault()
  const root = event.currentTarget.closest('[data-nutrition-editor]')
  const cells = Array.from(root?.querySelectorAll<HTMLInputElement>('input[data-amount]') ?? [])
  const next = cells[cells.indexOf(event.currentTarget) + 1]
  if (next) {
    next.focus()
    next.select()
  } else {
    event.currentTarget.blur()
  }
}
