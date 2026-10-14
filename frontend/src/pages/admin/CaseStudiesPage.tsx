import { useState } from 'react'

import {
  useAdminCaseStudies,
  useCaseStudyAnalysis,
  useCreateCaseStudy,
  useDeleteCaseStudy,
  useSetCaseStudyPublished,
  useUpdateCaseStudy,
} from '@/api/caseStudyApi'
import { CsvImportPanel } from '@/components/admin/CsvImportPanel'
import { AnalysisReport } from '@/components/caseStudy/AnalysisReport'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import type { CaseStudy, Sex, UpsertCaseStudyInput } from '@/types/caseStudy'

/**
 * The coach's own body data, imported from spreadsheets and published as a case study.
 *
 * <p>Nothing is public until the publish switch is thrown, which is what makes it safe to
 * upload a messy export and look at the analysis first. The analysis shown here is produced
 * by the same code path as the public page, so what the coach checks is what a reader gets.
 */
export function CaseStudiesPage() {
  const { data: studies, isLoading, isError, error } = useAdminCaseStudies()
  useSignOutOn401(error)

  const [selectedId, setSelectedId] = useState<string | null>(null)
  const selected = studies?.find((study) => study.id === selectedId) ?? null

  return (
    <>
      <PageHeading
        title="Vaka Çalışmaları"
        description="Kendi ölçüm ve beslenme verinizi içe aktarın, analizi kontrol edin, sonra yayınlayın. Yayınlanan analiz ana sayfadaki Tecrübelerim bölümünde görünür."
      />

      {isLoading ? (
        <div className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="text-sm text-accent-orange">{(error as Error).message}</p>
      ) : (
        <div className="space-y-6">
          {selected ? (
            <StudyDetail study={selected} onBack={() => setSelectedId(null)} />
          ) : (
            <StudyList studies={studies ?? []} onOpen={setSelectedId} />
          )}
        </div>
      )}
    </>
  )
}

function StudyList({
  studies,
  onOpen,
}: {
  studies: CaseStudy[]
  onOpen: (id: string) => void
}) {
  const createStudy = useCreateCaseStudy()
  const [draft, setDraft] = useState<UpsertCaseStudyInput>(emptyDraft())

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (draft.title.trim().length === 0) {
      return
    }
    createStudy.mutate(
      { ...draft, title: draft.title.trim() },
      { onSuccess: (created) => { setDraft(emptyDraft()); onOpen(created.id) } },
    )
  }

  return (
    <>
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">
          Yeni Vaka Çalışması
        </h2>
        <form className="space-y-4" onSubmit={submit}>
          <Field label="Başlık *">
            <input
              value={draft.title}
              onChange={(event) => setDraft({ ...draft, title: event.target.value })}
              type="text"
              placeholder="Örn: İki Yıllık Kendi Verim"
              className={inputClasses}
            />
          </Field>
          <Field label="Özet">
            <textarea
              value={draft.summary ?? ''}
              onChange={(event) => setDraft({ ...draft, summary: event.target.value || null })}
              rows={3}
              placeholder="Bu veri hangi dönemi kapsıyor, nasıl toplandı?"
              className={inputClasses}
            />
          </Field>
          <BodyFields draft={draft} onChange={setDraft} />
          {createStudy.isError ? (
            <p className="text-xs text-accent-orange">{(createStudy.error as Error).message}</p>
          ) : null}
          <Button type="submit" disabled={createStudy.isPending}>
            {createStudy.isPending ? 'Oluşturuluyor…' : 'Oluştur'}
          </Button>
        </form>
      </section>

      <section className="space-y-3">
        {studies.length === 0 ? (
          <p className="text-sm italic text-text-muted">Henüz vaka çalışması yok.</p>
        ) : (
          studies.map((study) => (
            <button
              key={study.id}
              type="button"
              onClick={() => onOpen(study.id)}
              className="flex w-full flex-wrap items-center justify-between gap-3 rounded-2xl border border-border bg-surface p-5 text-left transition-colors hover:border-border-strong"
            >
              <div className="min-w-0">
                <p className="truncate font-semibold text-text-primary">{study.title}</p>
                <p className="mt-0.5 text-xs text-text-muted">
                  /{study.slug} · {study.measurementCount} ölçüm · {study.intakeCount} beslenme kaydı
                </p>
              </div>
              <span
                className={`shrink-0 rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-wider ${
                  study.published
                    ? 'bg-primary/15 text-primary'
                    : 'bg-surface-high text-text-muted'
                }`}
              >
                {study.published ? 'Yayında' : 'Taslak'}
              </span>
            </button>
          ))
        )}
      </section>
    </>
  )
}

function StudyDetail({ study, onBack }: { study: CaseStudy; onBack: () => void }) {
  const updateStudy = useUpdateCaseStudy()
  const setPublished = useSetCaseStudyPublished()
  const deleteStudy = useDeleteCaseStudy()
  const { data: analysis, isLoading: analysisLoading } = useCaseStudyAnalysis(study.id)

  const [draft, setDraft] = useState<UpsertCaseStudyInput>(toDraft(study))
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button type="button" variant="ghost" onClick={onBack}>
          ← Listeye dön
        </Button>
        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            variant={study.published ? 'secondary' : 'primary'}
            disabled={setPublished.isPending}
            onClick={() => setPublished.mutate({ id: study.id, published: !study.published })}
          >
            {study.published ? 'Yayından kaldır' : 'Yayınla'}
          </Button>
          <Button type="button" variant="ghost" onClick={() => setConfirmingDelete(true)}>
            Sil
          </Button>
        </div>
      </div>

      {setPublished.isError ? (
        <p className="text-xs text-accent-orange">{(setPublished.error as Error).message}</p>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Bilgiler</h2>
        <form
          className="space-y-4"
          onSubmit={(event) => {
            event.preventDefault()
            updateStudy.mutate({ id: study.id, input: { ...draft, title: draft.title.trim() } })
          }}
        >
          <Field label="Başlık *">
            <input
              value={draft.title}
              onChange={(event) => setDraft({ ...draft, title: event.target.value })}
              type="text"
              className={inputClasses}
            />
          </Field>
          <Field label="Özet">
            <textarea
              value={draft.summary ?? ''}
              onChange={(event) => setDraft({ ...draft, summary: event.target.value || null })}
              rows={3}
              className={inputClasses}
            />
          </Field>
          <BodyFields draft={draft} onChange={setDraft} />
          <p className="text-xs text-text-muted">
            Adres (/{study.slug}) başlıktan bir kez üretilir ve değişmez; yayındaki bir bağlantı
            başlık düzeltilince kırılmasın diye.
          </p>
          {updateStudy.isError ? (
            <p className="text-xs text-accent-orange">{(updateStudy.error as Error).message}</p>
          ) : null}
          <Button type="submit" disabled={updateStudy.isPending}>
            {updateStudy.isPending ? 'Kaydediliyor…' : 'Kaydet'}
          </Button>
        </form>
      </section>

      <CsvImportPanel studyId={study.id} kind="MEASUREMENTS" rowCount={study.measurementCount} />
      <CsvImportPanel studyId={study.id} kind="INTAKE" rowCount={study.intakeCount} />

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
          Analiz Önizleme
        </h2>
        <p className="mb-5 text-xs text-text-muted">
          Yayındaki sayfanın göreceği analizin aynısı. Yayınlamadan önce buradan kontrol edin.
        </p>
        {analysisLoading ? (
          <div className="h-40 animate-pulse rounded-xl border border-border bg-surface-high" />
        ) : analysis ? (
          <AnalysisReport analysis={analysis} />
        ) : (
          <p className="text-sm italic text-text-muted">Analiz için veri yok.</p>
        )}
      </section>

      {confirmingDelete ? (
        <ConfirmDialog
          title="Vaka çalışması silinsin mi?"
          description={`"${study.title}" ve içe aktarılmış ${study.measurementCount} ölçüm, ${study.intakeCount} beslenme kaydı kalıcı olarak silinecek.`}
          confirmLabel="Sil"
          tone="danger"
          isPending={deleteStudy.isPending}
          error={deleteStudy.isError ? (deleteStudy.error as Error).message : null}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() => deleteStudy.mutate(study.id, { onSuccess: onBack })}
        />
      ) : null}
    </>
  )
}

/**
 * Height, sex and birth year exist only to feed the body-fat formulas, which is said on the
 * form rather than left to be guessed — without them the analysis simply leaves that
 * estimate out.
 */
function BodyFields({
  draft,
  onChange,
}: {
  draft: UpsertCaseStudyInput
  onChange: (next: UpsertCaseStudyInput) => void
}) {
  return (
    <>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Field label="Boy (cm)">
          <input
            value={draft.heightCm ?? ''}
            onChange={(event) =>
              onChange({ ...draft, heightCm: event.target.value ? Number(event.target.value) : null })
            }
            type="number"
            min={100}
            max={250}
            className={inputClasses}
          />
        </Field>
        <Field label="Cinsiyet">
          <select
            value={draft.sex ?? ''}
            onChange={(event) =>
              onChange({ ...draft, sex: event.target.value ? (event.target.value as Sex) : null })
            }
            className={inputClasses}
          >
            <option value="">— belirtilmedi —</option>
            <option value="ERKEK">Erkek</option>
            <option value="KADIN">Kadın</option>
          </select>
        </Field>
        <Field label="Doğum yılı">
          <input
            value={draft.birthYear ?? ''}
            onChange={(event) =>
              onChange({
                ...draft,
                birthYear: event.target.value ? Number(event.target.value) : null,
              })
            }
            type="number"
            min={1900}
            max={2100}
            className={inputClasses}
          />
        </Field>
      </div>
      <p className="text-xs text-text-muted">
        Bu üç alan yalnızca yağ oranı formülü için kullanılır. Boşsa analiz o tahmini atlar,
        uydurmaz. Doğum yılı yayınlanan sayfada görünmez.
      </p>
    </>
  )
}

function emptyDraft(): UpsertCaseStudyInput {
  return { title: '', summary: null, heightCm: null, sex: null, birthYear: null }
}

function toDraft(study: CaseStudy): UpsertCaseStudyInput {
  return {
    title: study.title,
    summary: study.summary,
    heightCm: study.heightCm,
    sex: study.sex,
    birthYear: study.birthYear,
  }
}
