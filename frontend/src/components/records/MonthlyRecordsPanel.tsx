import { useState, type FormEvent } from 'react'

import type { RecordSource } from '@/api/monthlyRecordApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { PencilIcon, PlusIcon, TrashIcon } from '@/components/ui/icons'
import { currentMonthIso } from '@/lib/date'
import {
  FIGURES,
  formatPeriod,
  parseFigure,
  type FigureKey,
  type MonthlyRecord,
  type MonthlyRecordInput,
} from '@/types/monthlyRecord'

const INPUT_CLASS =
  'h-10 w-full rounded-lg border border-border-strong bg-canvas px-3 text-right text-sm text-text-primary outline-none focus:border-primary'

/**
 * Month-by-month body figures as tables: one table per month, a pencil to change it, a bin to
 * remove it. Used for the member's own page and, with another source, for the coach.
 */
export function MonthlyRecordsPanel({ source }: { source: RecordSource }) {
  const [editing, setEditing] = useState<'new' | string | null>(null)
  const [deleting, setDeleting] = useState<MonthlyRecord | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const records = source.records ?? []

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

  return (
    <div className="space-y-6">
      {editing === null ? (
        <div>
          <Button onClick={() => setEditing('new')}>
            <PlusIcon className="h-4 w-4" />
            Yeni Ay Ekle
          </Button>
        </div>
      ) : null}

      {editing === 'new' ? (
        <RecordForm
          existing={records}
          source={source}
          onDone={() => setEditing(null)}
        />
      ) : null}

      {source.isLoading ? <p className="text-sm text-text-muted">Yükleniyor…</p> : null}
      {source.error ? (
        <p role="alert" className="text-sm text-error">
          {source.error.message}
        </p>
      ) : null}
      {!source.isLoading && !source.error && records.length === 0 && editing !== 'new' ? (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Henüz bir ay eklenmedi. “Yeni Ay Ekle” ile ilk tablonu oluştur.
        </p>
      ) : null}

      {records.map((record) =>
        editing === record.id ? (
          <RecordForm
            key={record.id}
            record={record}
            existing={records}
            source={source}
            onDone={() => setEditing(null)}
          />
        ) : (
          <RecordCard
            key={record.id}
            record={record}
            onEdit={() => setEditing(record.id)}
            onDelete={() => {
              setDeleteError(null)
              setDeleting(record)
            }}
          />
        ),
      )}

      {deleting ? (
        <ConfirmDialog
          title="Ayı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={isDeleting}
          error={deleteError}
          description={`${formatPeriod(deleting.period)} tablosunu silmek istediğinize emin misiniz?`}
          onCancel={() => setDeleting(null)}
          onConfirm={confirmDelete}
        />
      ) : null}
    </div>
  )
}

function RecordCard({
  record,
  onEdit,
  onDelete,
}: {
  record: MonthlyRecord
  onEdit: () => void
  onDelete: () => void
}) {
  const label = formatPeriod(record.period)
  return (
    <section className="rounded-2xl border border-border bg-surface p-5 sm:p-6">
      <header className="mb-4 flex items-center justify-between border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">{label}</h2>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onEdit}
            aria-label={`${label} verilerini düzenle`}
            className="text-text-muted transition-colors hover:text-primary"
          >
            <PencilIcon className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={onDelete}
            aria-label={`${label} verilerini sil`}
            className="text-text-muted transition-colors hover:text-error"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </div>
      </header>
      <table className="w-full border-collapse text-sm">
        <tbody>
          {FIGURES.map((figure) => {
            const value = record[figure.key]
            return (
              <tr key={figure.key} className="border-b border-border/60 last:border-0">
                <th scope="row" className="py-2 pr-4 text-left font-medium text-text-secondary">
                  {figure.label}
                </th>
                <td className="py-2 text-right text-text-primary">
                  {value === null ? '—' : `${value} ${figure.unit}`}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </section>
  )
}

function RecordForm({
  record,
  existing,
  source,
  onDone,
}: {
  record?: MonthlyRecord
  existing: MonthlyRecord[]
  source: RecordSource
  onDone: () => void
}) {
  const thisMonth = currentMonthIso()
  const [period, setPeriod] = useState(record?.period ?? thisMonth)
  const [values, setValues] = useState<Record<FigureKey, string>>(() =>
    Object.fromEntries(FIGURES.map((figure) => [figure.key, record?.[figure.key]?.toString() ?? ''])) as Record<
      FigureKey,
      string
    >,
  )
  const [error, setError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)

    if (!record) {
      if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(period)) {
        setError('Bir ay seçin.')
        return
      }
      if (existing.some((row) => row.period === period)) {
        setError(`${formatPeriod(period)} için zaten bir tablo var; kalem simgesiyle düzenleyin.`)
        return
      }
    }

    const input = {} as MonthlyRecordInput
    for (const figure of FIGURES) {
      const parsed = parseFigure(values[figure.key])
      if (parsed === 'invalid') {
        setError(`${figure.label} için geçerli bir sayı girin.`)
        return
      }
      input[figure.key] = parsed
    }
    if (FIGURES.every((figure) => input[figure.key] === null)) {
      setError('En az bir değer girin.')
      return
    }

    setIsSaving(true)
    try {
      if (record) {
        await source.update(record.id, input)
      } else {
        await source.create({ ...input, period })
      }
      onDone()
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Kaydedilemedi.')
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <form
      onSubmit={submit}
      noValidate
      className="rounded-2xl border border-primary/40 bg-surface p-5 sm:p-6"
    >
      <header className="mb-4 flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          {record ? `${formatPeriod(record.period)} verilerini düzenle` : 'Yeni ay'}
        </h2>
        {record ? null : (
          <label className="flex items-center gap-2 text-sm text-text-secondary">
            Ay
            <input
              type="month"
              value={period}
              max={thisMonth}
              onChange={(event) => setPeriod(event.target.value)}
              className="h-10 rounded-lg border border-border-strong bg-canvas px-3 text-sm text-text-primary outline-none focus:border-primary"
            />
          </label>
        )}
      </header>

      <table className="w-full border-collapse text-sm">
        <tbody>
          {FIGURES.map((figure) => (
            <tr key={figure.key} className="border-b border-border/60 last:border-0">
              <th scope="row" className="py-1.5 pr-4 text-left font-medium">
                <label htmlFor={`fig-${figure.key}`} className="text-text-secondary">
                  {figure.label}
                </label>
              </th>
              <td className="w-40 py-1.5">
                <div className="flex items-center gap-2">
                  <input
                    id={`fig-${figure.key}`}
                    type="text"
                    inputMode="decimal"
                    autoComplete="off"
                    value={values[figure.key]}
                    onChange={(event) => setValues((current) => ({ ...current, [figure.key]: event.target.value }))}
                    className={INPUT_CLASS}
                  />
                  <span className="w-6 text-xs text-text-muted">{figure.unit}</span>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {error ? (
        <p role="alert" className="mt-4 text-sm text-error">
          {error}
        </p>
      ) : null}

      <div className="mt-5 flex gap-3">
        <Button type="submit" disabled={isSaving}>
          {isSaving ? 'Kaydediliyor…' : 'Kaydet'}
        </Button>
        <Button type="button" variant="ghost" onClick={onDone} disabled={isSaving}>
          Vazgeç
        </Button>
      </div>
    </form>
  )
}
