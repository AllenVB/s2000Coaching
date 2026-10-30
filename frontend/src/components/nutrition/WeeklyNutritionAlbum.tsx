import { useMemo, useRef, useState, type FormEvent } from 'react'

import { useAuthedImage, type WeeklyNutritionSource } from '@/api/weeklyNutritionApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses } from '@/components/ui/FormField'
import { PencilIcon, TrashIcon } from '@/components/ui/icons'
import { formatAmount, formatWeek, macrosOf, recentWeeks } from '@/lib/weeklyNutrition'
import type { WeeklyNutrition } from '@/types/weeklyNutrition'

/** Mirrors `app.photos.max-size-bytes`; the server enforces it, this only says so earlier. */
const MAX_PHOTO_BYTES = 8 * 1024 * 1024
const WEEKS_OFFERED = 52
const MAX_CALORIES = 100_000

/**
 * The weekly food album: pick a Monday-to-Sunday week, enter the week's total calories, how they
 * split into carbohydrate, fat and protein (it has to come to 100%) and one photo; the grams and the
 * daily averages are worked out from that. With a source that cannot save (the coach's) only the
 * album is shown.
 */
export function WeeklyNutritionAlbum({ source }: { source: WeeklyNutritionSource }) {
  const entries = source.entries ?? []
  const [editingWeek, setEditingWeek] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<WeeklyNutrition | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const formRef = useRef<HTMLDivElement>(null)

  const confirmDelete = async () => {
    if (!deleting) {
      return
    }
    setIsDeleting(true)
    setDeleteError(null)
    try {
      await source.remove(deleting.id)
      setDeleting(null)
    } catch (error) {
      setDeleteError(error instanceof Error ? error.message : 'Silinemedi.')
    } finally {
      setIsDeleting(false)
    }
  }

  const edit = (entry: WeeklyNutrition) => {
    setEditingWeek(entry.weekStart)
    formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <div className="space-y-6">
      {source.save ? (
        <div ref={formRef}>
          <WeekForm
            // A new key restarts the form on the chosen week, so it starts from that week's saved values.
            key={editingWeek ?? 'new'}
            entries={entries}
            initialWeek={editingWeek}
            save={source.save}
            onCancel={() => setEditingWeek(null)}
          />
        </div>
      ) : null}

      {source.isLoading ? <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" /> : null}
      {source.error ? (
        <p role="alert" className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Haftalık beslenme yüklenemedi: {source.error.message}
        </p>
      ) : null}
      {!source.isLoading && !source.error && entries.length === 0 ? (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          {source.save
            ? 'Henüz bir hafta eklenmedi. Yukarıdan haftayı seçip ilk kaydınızı oluşturun.'
            : 'Üye henüz haftalık beslenme kaydı girmedi.'}
        </p>
      ) : null}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {entries.map((entry) => (
          <WeekCard
            key={entry.id}
            entry={entry}
            source={source}
            onEdit={source.save ? () => edit(entry) : undefined}
            onDelete={() => {
              setDeleteError(null)
              setDeleting(entry)
            }}
          />
        ))}
      </div>

      {deleting ? (
        <ConfirmDialog
          title="Haftayı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={isDeleting}
          error={deleteError}
          description={`${formatWeek(deleting.weekStart)} haftasının kaydını ve fotoğrafını silmek istediğinize emin misiniz?`}
          onCancel={() => setDeleting(null)}
          onConfirm={confirmDelete}
        />
      ) : null}
    </div>
  )
}

/** One saved week: the photo, what was entered, and the grams and daily averages that follow from it. */
function WeekCard({
  entry,
  source,
  onEdit,
  onDelete,
}: {
  entry: WeeklyNutrition
  source: WeeklyNutritionSource
  onEdit?: () => void
  onDelete: () => void
}) {
  const label = formatWeek(entry.weekStart)
  return (
    <section className="overflow-hidden rounded-2xl border border-border bg-surface">
      <header className="flex items-center justify-between gap-3 border-b border-border px-5 py-3.5">
        <h3 className="text-sm font-bold tracking-wide text-text-primary">{label}</h3>
        <div className="flex items-center gap-3">
          {onEdit ? (
            <button
              type="button"
              onClick={onEdit}
              aria-label={`${label} haftasını düzenle`}
              className="text-text-muted transition-colors hover:text-primary"
            >
              <PencilIcon className="h-4 w-4" />
            </button>
          ) : null}
          <button
            type="button"
            onClick={onDelete}
            aria-label={`${label} haftasını sil`}
            className="text-text-muted transition-colors hover:text-error"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </div>
      </header>

      <WeekPhoto entry={entry} source={source} label={label} />

      <div className="p-5">
        <p className="mb-3 text-xs text-text-secondary">
          Haftalık toplam{' '}
          <span className="text-sm font-bold text-text-primary">{formatAmount(entry.calories)} kcal</span>
        </p>
        <MacroTable
          rows={[
            { label: 'Karbonhidrat', percent: entry.carbsPercent, weeklyG: entry.weekly.carbsG, dailyG: entry.dailyAverage.carbsG },
            { label: 'Yağ', percent: entry.fatPercent, weeklyG: entry.weekly.fatG, dailyG: entry.dailyAverage.fatG },
            { label: 'Protein', percent: entry.proteinPercent, weeklyG: entry.weekly.proteinG, dailyG: entry.dailyAverage.proteinG },
          ]}
          weeklyKcal={entry.weekly.calories}
          dailyKcal={entry.dailyAverage.calories}
        />
      </div>
    </section>
  )
}

function WeekPhoto({ entry, source, label }: { entry: WeeklyNutrition; source: WeeklyNutritionSource; label: string }) {
  const { url, error } = useAuthedImage(entry.hasPhoto ? source.photoPath(entry) : '', entry.hasPhoto ? source.headers : null)

  if (!entry.hasPhoto) {
    return (
      <div className="flex h-24 items-center justify-center bg-surface-elevated text-[11px] italic text-text-muted">
        Bu hafta için fotoğraf yok
      </div>
    )
  }
  if (error) {
    return (
      <div className="flex h-40 items-center justify-center bg-error/10 p-4 text-center text-[11px] text-error">
        Fotoğraf yüklenemedi: {error.message}
      </div>
    )
  }
  if (!url) {
    return <div className="h-56 animate-pulse bg-surface-elevated" />
  }
  return <img src={url} alt={`${label} haftasının yemek fotoğrafı`} className="h-56 w-full object-cover" />
}

interface MacroRow {
  label: string
  percent: number
  weeklyG: number
  dailyG: number
}

/** Percent, the week's grams and the daily average for each macro, with the calories as the last row. */
function MacroTable({ rows, weeklyKcal, dailyKcal }: { rows: MacroRow[]; weeklyKcal: number; dailyKcal: number }) {
  return (
    <table className="w-full border-collapse text-sm">
      <thead>
        <tr className="border-b border-border text-[11px] font-semibold uppercase tracking-wider text-text-muted">
          <th className="py-2 text-left font-semibold" />
          <th className="py-2 text-right font-semibold">Yüzde</th>
          <th className="py-2 text-right font-semibold">Haftalık</th>
          <th className="py-2 text-right font-semibold">Günlük ort.</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.label} className="border-b border-border/60">
            <th scope="row" className="py-2 text-left font-medium text-text-secondary">
              {row.label}
            </th>
            <td className="py-2 text-right text-text-secondary">%{row.percent}</td>
            <td className="py-2 text-right text-text-primary">{formatAmount(row.weeklyG)} g</td>
            <td className="py-2 text-right font-semibold text-text-primary">{formatAmount(row.dailyG)} g</td>
          </tr>
        ))}
        <tr>
          <th scope="row" className="py-2 text-left font-medium text-text-secondary">
            Kalori
          </th>
          <td className="py-2 text-right text-text-secondary">%100</td>
          <td className="py-2 text-right text-text-primary">{formatAmount(weeklyKcal)} kcal</td>
          <td className="py-2 text-right font-semibold text-primary">{formatAmount(dailyKcal)} kcal</td>
        </tr>
      </tbody>
    </table>
  )
}

/** A whole number in a text box; empty is `null`, anything else that is not a whole number is `NaN`. */
function parseWhole(text: string): number | null {
  const trimmed = text.trim()
  if (trimmed === '') {
    return null
  }
  return /^\d+$/.test(trimmed) ? Number(trimmed) : Number.NaN
}

function WeekForm({
  entries,
  initialWeek,
  save,
  onCancel,
}: {
  entries: WeeklyNutrition[]
  initialWeek: string | null
  save: NonNullable<WeeklyNutritionSource['save']>
  /** Leaves the edit that a card's pencil started. */
  onCancel: () => void
}) {
  const weeks = useMemo(() => recentWeeks(WEEKS_OFFERED), [])
  const byWeek = useMemo(() => new Map(entries.map((entry) => [entry.weekStart, entry])), [entries])

  const [weekStart, setWeekStart] = useState(initialWeek ?? weeks[0])
  const existing = byWeek.get(weekStart)
  const [calories, setCalories] = useState(existing ? String(existing.calories) : '')
  const [carbs, setCarbs] = useState(existing ? String(existing.carbsPercent) : '')
  const [fat, setFat] = useState(existing ? String(existing.fatPercent) : '')
  const [protein, setProtein] = useState(existing ? String(existing.proteinPercent) : '')
  const [photo, setPhoto] = useState<File | null>(null)
  const [removePhoto, setRemovePhoto] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [savedWeek, setSavedWeek] = useState<string | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)

  // Choosing another week loads what is saved for it (or empties the form), so a saved week is
  // corrected rather than started over.
  const chooseWeek = (next: string) => {
    setWeekStart(next)
    const saved = byWeek.get(next)
    setCalories(saved ? String(saved.calories) : '')
    setCarbs(saved ? String(saved.carbsPercent) : '')
    setFat(saved ? String(saved.fatPercent) : '')
    setProtein(saved ? String(saved.proteinPercent) : '')
    setPhoto(null)
    setRemovePhoto(false)
    setError(null)
    setSavedWeek(null)
    if (fileInput.current) {
      fileInput.current.value = ''
    }
  }

  const caloriesValue = parseWhole(calories)
  const carbsValue = parseWhole(carbs)
  const fatValue = parseWhole(fat)
  const proteinValue = parseWhole(protein)
  const percents = [carbsValue, fatValue, proteinValue]
  const percentsValid = percents.every((value) => value !== null && !Number.isNaN(value) && value <= 100)
  const total = percents.reduce<number>((sum, value) => sum + (value !== null && !Number.isNaN(value) ? value : 0), 0)
  const caloriesValid = caloriesValue !== null && !Number.isNaN(caloriesValue) && caloriesValue >= 1 && caloriesValue <= MAX_CALORIES
  const complete = caloriesValid && percentsValid && total === 100

  const preview =
    complete && caloriesValue !== null
      ? { weekly: macrosOf(caloriesValue, carbsValue!, fatValue!, proteinValue!), daily: macrosOf(caloriesValue, carbsValue!, fatValue!, proteinValue!, 7) }
      : null

  const pickPhoto = (picked: File | null) => {
    setError(null)
    if (picked && picked.size > MAX_PHOTO_BYTES) {
      setError('Fotoğraf en fazla 8 MB olabilir.')
      setPhoto(null)
      if (fileInput.current) {
        fileInput.current.value = ''
      }
      return
    }
    setPhoto(picked)
    if (picked) {
      setRemovePhoto(false)
    }
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    if (!caloriesValid || caloriesValue === null) {
      setError(`Haftalık toplam kaloriyi 1 ile ${formatAmount(MAX_CALORIES)} arasında bir tam sayı olarak girin.`)
      return
    }
    if (!percentsValid) {
      setError('Karbonhidrat, yağ ve protein yüzdelerini 0 ile 100 arasında tam sayı olarak girin.')
      return
    }
    if (total !== 100) {
      setError(`Yüzdelerin toplamı 100 olmalı (şu an ${total}).`)
      return
    }

    setIsSaving(true)
    try {
      await save({
        weekStart,
        calories: caloriesValue,
        carbsPercent: carbsValue!,
        fatPercent: fatValue!,
        proteinPercent: proteinValue!,
        photo,
        removePhoto,
      })
      // The form keeps showing what was just saved (it is now that week's entry); only the picked
      // photo is spent.
      setPhoto(null)
      setRemovePhoto(false)
      setSavedWeek(weekStart)
      if (fileInput.current) {
        fileInput.current.value = ''
      }
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Kaydedilemedi.')
    } finally {
      setIsSaving(false)
    }
  }

  const totalNote =
    total === 100
      ? { text: 'Toplam %100', tone: 'text-success' }
      : total < 100
        ? { text: `Toplam %${total} — ${100 - total} puan eksik`, tone: 'text-text-muted' }
        : { text: `Toplam %${total} — ${total - 100} puan fazla`, tone: 'text-error' }

  return (
    <form onSubmit={submit} noValidate className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-5 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Haftalık Beslenme</h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Haftayı (pazartesi–pazar) seçin, o haftanın toplam kalorisini ve karbonhidrat, yağ, protein
          yüzdelerini girin; yüzdelerin toplamı 100 olmalı. Gramajlar otomatik hesaplanır. Haftaya tek bir
          fotoğraf ekleyebilirsiniz; yalnızca siz ve koçunuz görür.
        </p>
      </div>

      <div className="space-y-4">
        <Field label="Hafta *">
          <select value={weekStart} onChange={(event) => chooseWeek(event.target.value)} className={inputClasses}>
            {weeks.map((week, index) => (
              <option key={week} value={week}>
                {formatWeek(week)}
                {index === 0 ? ' (bu hafta)' : ''}
                {byWeek.has(week) ? ' · kayıtlı' : ''}
              </option>
            ))}
          </select>
        </Field>
        {existing ? (
          <p className="text-[11px] text-text-muted">Bu hafta için kayıt var; kaydederseniz güncellenir.</p>
        ) : null}

        <Field label="Haftalık toplam kalori (kcal) *">
          <input
            value={calories}
            onChange={(event) => setCalories(event.target.value)}
            type="text"
            inputMode="numeric"
            autoComplete="off"
            placeholder="örn. 14000"
            className={inputClasses}
          />
        </Field>

        <div className="grid grid-cols-3 gap-3">
          <Field label="Karbonhidrat % *">
            <input value={carbs} onChange={(event) => setCarbs(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
          </Field>
          <Field label="Yağ % *">
            <input value={fat} onChange={(event) => setFat(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
          </Field>
          <Field label="Protein % *">
            <input value={protein} onChange={(event) => setProtein(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
          </Field>
        </div>
        <p role="status" className={`text-xs font-semibold ${totalNote.tone}`}>
          {totalNote.text}
        </p>

        {preview ? (
          <MacroTable
            rows={[
              { label: 'Karbonhidrat', percent: carbsValue!, weeklyG: preview.weekly.carbsG, dailyG: preview.daily.carbsG },
              { label: 'Yağ', percent: fatValue!, weeklyG: preview.weekly.fatG, dailyG: preview.daily.fatG },
              { label: 'Protein', percent: proteinValue!, weeklyG: preview.weekly.proteinG, dailyG: preview.daily.proteinG },
            ]}
            weeklyKcal={preview.weekly.calories}
            dailyKcal={preview.daily.calories}
          />
        ) : null}

        <Field label="Fotoğraf" hint="Haftaya tek fotoğraf · JPEG, PNG veya WebP · en fazla 8 MB">
          <input
            ref={fileInput}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => pickPhoto(event.target.files?.[0] ?? null)}
            className="block w-full text-xs text-text-secondary file:mr-3 file:h-9 file:cursor-pointer file:rounded-lg file:border-0 file:bg-surface-elevated file:px-3.5 file:text-xs file:font-semibold file:uppercase file:tracking-wide file:text-text-primary"
          />
        </Field>
        {existing?.hasPhoto ? (
          <label className="flex items-center gap-2 text-xs text-text-secondary">
            <input
              type="checkbox"
              checked={removePhoto}
              disabled={photo !== null}
              onChange={(event) => setRemovePhoto(event.target.checked)}
            />
            Bu haftanın mevcut fotoğrafını kaldır
            {photo ? ' (yeni fotoğraf onun yerine geçecek)' : ''}
          </label>
        ) : null}

        {error ? (
          <p role="alert" className="text-xs text-error">
            {error}
          </p>
        ) : savedWeek === weekStart ? (
          <p role="status" className="text-xs text-success">
            Kaydedildi; hesaplanan değerler aşağıdaki kartta.
          </p>
        ) : null}

        <div className="flex flex-wrap gap-3">
          <Button type="submit" disabled={isSaving || !complete}>
            {isSaving ? 'Kaydediliyor…' : existing ? 'Güncelle' : 'Kaydet'}
          </Button>
          {initialWeek ? (
            <Button type="button" variant="ghost" onClick={onCancel} disabled={isSaving}>
              Vazgeç
            </Button>
          ) : null}
        </div>
      </div>
    </form>
  )
}
