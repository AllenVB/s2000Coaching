import { useState } from 'react'

import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import { MEASURE_FIELDS, type Measurement } from '@/types/measurement'

/** The table view the chart needs: every recorded value, readable without colour. */
export function MeasurementHistory({
  measurements,
  onDelete,
  isDeleting = false,
  deleteError,
}: {
  measurements: Measurement[]
  onDelete?: (measurementId: string) => void
  isDeleting?: boolean
  deleteError?: string | null
}) {
  const [deletingId, setDeletingId] = useState<string | null>(null)

  if (measurements.length === 0) {
    return (
      <section className="rounded-2xl border border-border bg-surface p-6">
        <p className="text-sm italic text-text-muted">Henüz ölçüm kaydı yok.</p>
      </section>
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
        Ölçüm Geçmişi
      </h2>

      <div className="-mx-2 overflow-x-auto">
        <table className="w-full min-w-[520px] border-collapse text-sm">
          <thead>
            <tr className="border-b border-border text-left">
              <th className="px-2 py-2 text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                Tarih
              </th>
              {MEASURE_FIELDS.map((field) => (
                <th
                  key={field.key}
                  className="px-2 py-2 text-right text-[11px] font-semibold uppercase tracking-wider text-text-muted"
                >
                  {field.label}
                </th>
              ))}
              {onDelete ? <th className="w-10 px-2 py-2" /> : null}
            </tr>
          </thead>
          <tbody>
            {measurements.map((row) => (
              <tr key={row.id} className="border-b border-border/60 last:border-0">
                <td className="whitespace-nowrap px-2 py-2.5 text-text-primary">
                  {formatDate(row.measuredOn)}
                </td>
                {MEASURE_FIELDS.map((field) => (
                  <td key={field.key} className="px-2 py-2.5 text-right text-text-secondary">
                    {row[field.key] === null ? '—' : row[field.key]}
                  </td>
                ))}
                {onDelete ? (
                  <td className="px-2 py-2.5 text-right">
                    <button
                      type="button"
                      onClick={() => setDeletingId(row.id)}
                      aria-label={`${formatDate(row.measuredOn)} ölçümünü sil`}
                      className="text-text-muted transition-colors hover:text-error"
                    >
                      <TrashIcon className="h-4 w-4" />
                    </button>
                  </td>
                ) : null}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {measurements.some((row) => row.note) ? (
        <ul className="mt-5 space-y-2 border-t border-border pt-4">
          {measurements
            .filter((row) => row.note)
            .map((row) => (
              <li key={row.id} className="text-xs leading-relaxed text-text-secondary">
                <span className="font-semibold text-text-muted">{formatDate(row.measuredOn)}: </span>
                {row.note}
              </li>
            ))}
        </ul>
      ) : null}

      {deletingId && onDelete ? (
        <ConfirmDialog
          title="Ölçümü Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={isDeleting}
          error={deleteError ?? null}
          description="Bu ölçüm kaydını silmek istediğinize emin misiniz?"
          onCancel={() => setDeletingId(null)}
          onConfirm={() => {
            onDelete(deletingId)
            setDeletingId(null)
          }}
        />
      ) : null}
    </section>
  )
}
