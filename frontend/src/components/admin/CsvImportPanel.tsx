import { useRef, useState } from 'react'

import { useClearCaseStudyData, useImportCsv, usePreviewCsv } from '@/api/caseStudyApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import {
  FIELD_LABELS,
  REQUIRED_FIELDS,
  type CsvPreview,
  type DataKind,
  type ImportResult,
} from '@/types/caseStudy'

const KIND_TITLES: Record<DataKind, string> = {
  MEASUREMENTS: 'Ölçümler',
  INTAKE: 'Günlük beslenme kaydı',
}

const KIND_HINTS: Record<DataKind, string> = {
  MEASUREMENTS: 'Tarih ve kilo zorunlu. Boyun ve bel ölçüsü varsa yağ oranı tahmini yapılabilir.',
  INTAKE: 'Tarih ve kalori zorunlu. Protein, karbonhidrat, yağ ve sodyum varsa eklenir.',
}

/**
 * Upload in two steps: read the file, check the mapping, then import.
 *
 * <p>The middle step is the whole point. The sample rows are shown exactly as the parser
 * read them, because a decimal comma taken for a thousands separator turns 80,4 kg into 804
 * and nothing downstream will ever look wrong — the chart will just be strange. Seeing the
 * parsed value next to the heading is the only cheap moment to catch it.
 */
export function CsvImportPanel({
  studyId,
  kind,
  rowCount,
}: {
  studyId: string
  kind: DataKind
  rowCount: number
}) {
  const previewCsv = usePreviewCsv()
  const importCsv = useImportCsv()
  const clearData = useClearCaseStudyData()

  const fileInput = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<CsvPreview | null>(null)
  const [mapping, setMapping] = useState<Record<string, number>>({})
  const [result, setResult] = useState<ImportResult | null>(null)
  const [confirmingClear, setConfirmingClear] = useState(false)

  const reset = () => {
    setFile(null)
    setPreview(null)
    setMapping({})
    if (fileInput.current) {
      fileInput.current.value = ''
    }
  }

  const chooseFile = (chosen: File | null) => {
    setResult(null)
    setPreview(null)
    setMapping({})
    setFile(chosen)
    if (!chosen) {
      return
    }
    previewCsv.mutate(
      { id: studyId, kind, file: chosen },
      { onSuccess: (data) => { setPreview(data); setMapping(data.guessedMapping) } },
    )
  }

  const setField = (field: string, column: string) => {
    setMapping((old) => {
      const next = { ...old }
      if (column === '') {
        delete next[field]
      } else {
        next[field] = Number(column)
      }
      return next
    })
  }

  const missingRequired = REQUIRED_FIELDS[kind].filter((field) => !(field in mapping))
  // Two fields reading the same column is almost always a mis-set dropdown, and it would
  // store the same number twice without complaint.
  const duplicateColumns = Object.values(mapping).filter(
    (column, index, all) => all.indexOf(column) !== index,
  )

  const runImport = () => {
    if (!file || missingRequired.length > 0) {
      return
    }
    importCsv.mutate(
      { id: studyId, kind, file, mapping },
      { onSuccess: (data) => { setResult(data); reset() } },
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-1 flex flex-wrap items-center justify-between gap-3">
        <h3 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          {KIND_TITLES[kind]}
        </h3>
        <span className="text-xs text-text-muted">{rowCount} kayıt</span>
      </div>
      <p className="mb-4 text-xs text-text-muted">{KIND_HINTS[kind]}</p>

      <input
        ref={fileInput}
        type="file"
        accept=".csv,.txt,text/csv"
        onChange={(event) => chooseFile(event.target.files?.[0] ?? null)}
        className={`${inputClasses} cursor-pointer file:mr-3 file:rounded-lg file:border-0 file:bg-surface-high file:px-3 file:py-1.5 file:text-xs file:font-semibold file:text-text-primary`}
      />

      {previewCsv.isPending ? (
        <p className="mt-3 text-xs text-text-muted">Dosya okunuyor…</p>
      ) : null}
      {previewCsv.isError ? (
        <p className="mt-3 text-xs text-accent-orange">{(previewCsv.error as Error).message}</p>
      ) : null}

      {preview ? (
        <div className="mt-5 space-y-5">
          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-text-secondary">
              Sütun eşlemesi
            </p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {preview.availableFields.map((field) => {
                const required = REQUIRED_FIELDS[kind].includes(field)
                const value = mapping[field]
                return (
                  <label key={field} className="flex flex-col gap-1">
                    <span className="text-xs text-text-secondary">
                      {FIELD_LABELS[field] ?? field}
                      {required ? ' *' : ''}
                    </span>
                    <select
                      value={value === undefined ? '' : String(value)}
                      onChange={(event) => setField(field, event.target.value)}
                      className={inputClasses}
                    >
                      <option value="">— yok —</option>
                      {preview.headers.map((header, index) => (
                        <option key={index} value={index}>
                          {header || `(${index + 1}. sütun)`}
                        </option>
                      ))}
                    </select>
                  </label>
                )
              })}
            </div>
          </div>

          <div>
            <p className="mb-2 text-xs font-semibold uppercase tracking-wider text-text-secondary">
              Dosyadan ilk satırlar ({preview.totalRows} satır bulundu)
            </p>
            <div className="overflow-x-auto rounded-xl border border-border">
              <table className="w-full text-left text-xs">
                <thead className="bg-surface-high text-text-secondary">
                  <tr>
                    {preview.headers.map((header, index) => (
                      <th key={index} className="whitespace-nowrap px-3 py-2 font-semibold">
                        {header || `(${index + 1})`}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="text-text-primary">
                  {preview.sampleRows.map((row, rowIndex) => (
                    <tr key={rowIndex} className="border-t border-border">
                      {preview.headers.map((_header, columnIndex) => (
                        <td key={columnIndex} className="whitespace-nowrap px-3 py-2">
                          {row[columnIndex] ?? ''}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {missingRequired.length > 0 ? (
            <p className="text-xs text-accent-orange">
              Zorunlu alan eşlenmedi: {missingRequired.map((f) => FIELD_LABELS[f] ?? f).join(', ')}
            </p>
          ) : null}
          {duplicateColumns.length > 0 ? (
            <p className="text-xs text-accent-orange">
              Aynı sütun birden fazla alana eşlendi. Büyük olasılıkla yanlış seçim yapıldı.
            </p>
          ) : null}
          {importCsv.isError ? (
            <p className="text-xs text-accent-orange">{(importCsv.error as Error).message}</p>
          ) : null}

          <div className="flex flex-wrap gap-3">
            <Button
              type="button"
              onClick={runImport}
              disabled={missingRequired.length > 0 || importCsv.isPending}
            >
              {importCsv.isPending ? 'Aktarılıyor…' : 'İçe aktar'}
            </Button>
            <Button type="button" variant="ghost" onClick={reset}>
              Vazgeç
            </Button>
          </div>
        </div>
      ) : null}

      {result ? (
        <div className="mt-5 rounded-xl border border-border bg-surface-high p-4 text-xs">
          <p className="font-semibold text-text-primary">
            {result.importedRows} yeni, {result.updatedRows} güncellendi, {result.skippedRows} atlandı.
          </p>
          {result.firstDate ? (
            <p className="mt-1 text-text-secondary">
              Kapsanan aralık: {formatDate(result.firstDate)} – {formatDate(result.lastDate)}
            </p>
          ) : null}
          {result.warnings.length > 0 ? (
            <ul className="mt-2 max-h-40 space-y-0.5 overflow-y-auto text-text-muted">
              {result.warnings.map((warning, index) => (
                <li key={index}>• {warning}</li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}

      {rowCount > 0 ? (
        <div className="mt-5 border-t border-border pt-4">
          <p className="mb-2 text-xs text-text-muted">
            Yanlış eşlenmiş bir dosyayı düzeltilmişini yükleyerek geri alamazsınız: hatalı satırlar
            yeni dosyada olmayan tarihlerde durur. Böyle bir durumda bu veriyi tümüyle silip
            yeniden aktarın.
          </p>
          <Button type="button" variant="ghost" onClick={() => setConfirmingClear(true)}>
            {KIND_TITLES[kind]} verisini sil
          </Button>
        </div>
      ) : null}

      {confirmingClear ? (
        <ConfirmDialog
          title={`${KIND_TITLES[kind]} verisi silinsin mi?`}
          description={`${rowCount} kayıt kalıcı olarak silinecek. Bu işlem geri alınamaz.`}
          confirmLabel="Sil"
          tone="danger"
          isPending={clearData.isPending}
          error={clearData.isError ? (clearData.error as Error).message : null}
          onCancel={() => setConfirmingClear(false)}
          onConfirm={() => {
            clearData.mutate(
              { id: studyId, kind },
              { onSuccess: () => { setConfirmingClear(false); setResult(null) } },
            )
          }}
        />
      ) : null}
    </section>
  )
}

function formatDate(date: string | null): string {
  return date ? new Date(date).toLocaleDateString('tr-TR') : '—'
}
