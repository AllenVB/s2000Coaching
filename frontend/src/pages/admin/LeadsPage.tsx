import { useState } from 'react'

import { useDeleteLead, useLeads, useSetLeadStatus, type Lead } from '@/api/leadsApi'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDateTime } from '@/lib/date'

/** Free pre-assessment requests from the home page: who to call back, what they want, and who is done. */
export function LeadsPage() {
  const { data: leads, isLoading, isError, error } = useLeads()
  useSignOutOn401(error)
  const setStatus = useSetLeadStatus()
  const remove = useDeleteLead()
  const [deleting, setDeleting] = useState<Lead | null>(null)
  const open = leads?.filter((lead) => lead.status === 'NEW').length ?? 0

  return (
    <>
      <PageHeading
        title="Başvurular"
        description={`Ana sayfadaki ücretsiz ön değerlendirme formundan gelenler. ${open > 0 ? `${open} yeni başvuru dönüş bekliyor.` : 'Dönüş bekleyen başvuru yok.'}`}
      />
      {isLoading ? (
        <div className="h-24 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">Yüklenemedi: {error.message}</p>
      ) : !leads || leads.length === 0 ? (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">Henüz başvuru yok.</p>
      ) : (
        <ul className="space-y-3">
          {leads.map((lead) => (
            <li key={lead.id} className={`rounded-2xl border bg-surface p-5 ${lead.status === 'NEW' ? 'border-primary/50' : 'border-border'}`}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="font-semibold text-text-primary">
                    {lead.fullName}
                    {lead.status === 'NEW' ? <span className="ml-2 rounded-full bg-primary/15 px-2 py-0.5 text-[10px] font-bold uppercase text-primary">Yeni</span> : null}
                  </p>
                  <p className="mt-0.5 text-xs text-text-muted">{formatDateTime(lead.createdAt)}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <a href={`tel:${lead.phone}`} className="rounded-lg border border-border-strong px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary hover:text-text-primary">
                    Ara
                  </a>
                  <a
                    href={`https://wa.me/${lead.phone.replace(/[^0-9]/g, '').replace(/^0/, '90')}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="rounded-lg border border-border-strong px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary hover:text-text-primary"
                  >
                    WhatsApp
                  </a>
                </div>
              </div>
              <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-1.5 text-xs sm:grid-cols-3">
                <Info label="Telefon" value={lead.phone} />
                <Info label="E-posta" value={lead.email} />
                <Info label="Hedef" value={lead.goal} />
                <Info label="Spor geçmişi" value={lead.level} />
                <Info label="Boy" value={lead.heightCm ? `${lead.heightCm} cm` : null} />
                <Info label="Kilo" value={lead.weightKg ? `${lead.weightKg} kg` : null} />
              </dl>
              {lead.note ? <p className="mt-3 whitespace-pre-line border-t border-border pt-3 text-xs leading-relaxed text-text-secondary">{lead.note}</p> : null}
              <div className="mt-4 flex gap-2">
                <button
                  type="button"
                  disabled={setStatus.isPending}
                  onClick={() => setStatus.mutate({ id: lead.id, status: lead.status === 'NEW' ? 'CONTACTED' : 'NEW' })}
                  className="text-xs font-semibold uppercase tracking-wide text-primary hover:underline"
                >
                  {lead.status === 'NEW' ? 'Dönüş yapıldı olarak işaretle' : 'Yeniye çevir'}
                </button>
                <button type="button" onClick={() => setDeleting(lead)} className="text-xs text-text-muted underline hover:text-error">
                  Sil
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {deleting ? (
        <ConfirmDialog
          title="Başvuru silinsin mi?"
          description={`${deleting.fullName} adlı kişinin başvurusu ve iletişim bilgileri kalıcı olarak silinir.`}
          confirmLabel="Sil"
          tone="danger"
          isPending={remove.isPending}
          error={remove.isError ? remove.error.message : null}
          onCancel={() => setDeleting(null)}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      ) : null}
    </>
  )
}

function Info({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <dt className="text-[11px] text-text-muted">{label}</dt>
      <dd className="text-text-primary">{value ?? '—'}</dd>
    </div>
  )
}
