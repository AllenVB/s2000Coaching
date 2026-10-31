import { useState } from 'react'

import { useDeleteMyActivity, useMyActivity, useSaveMyActivity } from '@/api/activityApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate, todayIso } from '@/lib/date'
import type { Activity } from '@/types/activity'

function wholeOrNull(text: string, max: number): number | null | 'invalid' {
  if (text.trim() === '') {
    return null
  }
  const value = Number(text.trim().replace(/\./g, ''))
  return Number.isInteger(value) && value >= 0 && value <= max ? value : 'invalid'
}

/** Daily steps and cardio minutes; a past day is entered by its date, the future cannot be. */
export function ActivityLog() {
  const { data: rows } = useMyActivity()
  const [date, setDate] = useState(todayIso())
  const existing = rows?.find((row) => row.performedOn === date)
  const [deleting, setDeleting] = useState<Activity | null>(null)
  const remove = useDeleteMyActivity()

  return (
    <div className="space-y-6">
      {rows ? (
        <ActivityForm key={date} date={date} onDate={setDate} existing={existing} />
      ) : (
        <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
      )}

      {rows && rows.length > 0 ? (
        <section className="rounded-2xl border border-border bg-surface p-5 sm:p-6">
          <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Girdiğim Günler</h2>
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr className="border-b border-border text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                <th className="px-2 py-2 text-left">Tarih</th>
                <th className="px-2 py-2 text-right">Adım</th>
                <th className="px-2 py-2 text-right">Kardiyo</th>
                <th className="w-20 px-2" />
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} className="border-b border-border/60">
                  <td className="px-2 py-2 text-text-primary">{formatDate(row.performedOn)}</td>
                  <td className="px-2 py-2 text-right text-text-secondary">
                    {row.steps !== null ? row.steps.toLocaleString('tr-TR') : '—'}
                  </td>
                  <td className="px-2 py-2 text-right text-text-secondary">
                    {row.cardioMinutes !== null ? `${row.cardioMinutes} dk` : '—'}
                  </td>
                  <td className="px-2 py-2 text-right">
                    <button
                      type="button"
                      onClick={() => setDate(row.performedOn)}
                      className="mr-3 text-xs font-semibold uppercase tracking-wide text-primary hover:underline"
                      aria-label={`${formatDate(row.performedOn)} kaydını düzenle`}
                    >
                      Düzenle
                    </button>
                    <button
                      type="button"
                      onClick={() => setDeleting(row)}
                      aria-label={`${formatDate(row.performedOn)} kaydını sil`}
                      className="align-middle text-text-muted transition-colors hover:text-error"
                    >
                      <TrashIcon className="inline h-4 w-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : null}

      {deleting ? (
        <ConfirmDialog
          title="Kaydı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={remove.isPending}
          error={remove.isError ? remove.error.message : null}
          description={`${formatDate(deleting.performedOn)} tarihli adım/kardiyo kaydını silmek istediğinize emin misiniz?`}
          onCancel={() => setDeleting(null)}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      ) : null}
    </div>
  )
}

function ActivityForm({ date, onDate, existing }: { date: string; onDate: (date: string) => void; existing: Activity | undefined }) {
  const save = useSaveMyActivity()
  const [steps, setSteps] = useState(existing?.steps != null ? String(existing.steps) : '')
  const [cardio, setCardio] = useState(existing?.cardioMinutes != null ? String(existing.cardioMinutes) : '')
  const [error, setError] = useState<string | null>(null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    const stepsValue = wholeOrNull(steps, 200000)
    const cardioValue = wholeOrNull(cardio, 1440)
    if (stepsValue === 'invalid' || cardioValue === 'invalid') {
      setError('Adım 0–200.000, kardiyo süresi 0–1440 dakika arasında bir tam sayı olmalı.')
      return
    }
    if (stepsValue === null && cardioValue === null) {
      setError('Adım sayısı ya da kardiyo süresinden en az birini yazın.')
      return
    }
    save.mutate({ performedOn: date, steps: stepsValue, cardioMinutes: cardioValue })
  }

  return (
    <form onSubmit={submit} noValidate className="rounded-2xl border border-border bg-surface p-5 sm:p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Günlük Adım ve Kardiyo</h2>
      <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
        O gün attığınız adım sayısını ve yaptığınız kardiyonun süresini (dakika) yazın. Geçmiş bir günü girmek için
        tarihi değiştirin; aynı güne tekrar kaydederseniz güncellenir.
      </p>
      <div className="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Field label="Tarih *">
          <input type="date" value={date} max={todayIso()} onChange={(e) => e.target.value && onDate(e.target.value)} className={inputClasses} />
        </Field>
        <Field label="Adım sayısı">
          <input value={steps} onChange={(e) => setSteps(e.target.value)} type="text" inputMode="numeric" autoComplete="off" placeholder="örn. 9500" className={inputClasses} />
        </Field>
        <Field label="Kardiyo süresi (dk)">
          <input value={cardio} onChange={(e) => setCardio(e.target.value)} type="text" inputMode="numeric" autoComplete="off" placeholder="örn. 30" className={inputClasses} />
        </Field>
      </div>
      {existing ? <p className="mt-3 text-xs text-warning">Bu tarih için kayıt var; kaydederseniz güncellenir.</p> : null}
      {error ? <p role="alert" className="mt-3 text-xs text-error">{error}</p> : null}
      {save.isError ? <p className="mt-3 text-xs text-error">{save.error.message}</p> : null}
      {save.isSuccess ? <p className="mt-3 text-xs text-success">Kaydedildi.</p> : null}
      <Button type="submit" className="mt-4" disabled={save.isPending}>
        {save.isPending ? 'Kaydediliyor…' : existing ? 'Güncelle' : 'Kaydet'}
      </Button>
    </form>
  )
}
