import { Link, NavLink, Outlet, useSearchParams } from 'react-router-dom'

import { useMember } from '@/api/adminMembersApi'
import { StudentPicker } from '@/components/admin/StudentPicker'
import { PageHeading } from '@/components/ui/PageHeading'

/**
 * "Programlar": pick a student (search by name, e-mail, phone or #number), then write their training
 * and nutrition programmes under two tabs on the same page. Each tab is the member's own programme
 * editor — start from a ready template and edit it, or from nothing — so what is done here is exactly
 * what the member's page shows. The chosen student lives in the address (`?uye=`), so a refresh or a
 * link keeps the place.
 */
export function ProgramsPage() {
  const [params, setParams] = useSearchParams()
  const selectedId = params.get('uye') ?? undefined
  const { data: member } = useMember(selectedId)

  const tabClass = ({ isActive }: { isActive: boolean }) =>
    `rounded-full px-5 py-2 text-xs font-semibold uppercase tracking-wide transition-colors ${
      isActive ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'
    }`

  return (
    <>
      <PageHeading
        title="Programlar"
        description="Öğrenciyi seçin; antrenman ve beslenme programını aynı sayfada, üstteki sekmelerle yazın. Hazır bir şablonu kopyalayıp düzenleyebilir ya da sıfırdan başlayabilirsiniz."
        action={
          <Link to="/yonetim/sablonlar" className="text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
            Şablonları yönet
          </Link>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[300px_minmax(0,1fr)]">
        <StudentPicker selectedId={selectedId} onSelect={(id) => setParams({ uye: id }, { replace: true })} />

        <section className="min-w-0">
          {member ? (
            <>
              <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 className="text-lg font-bold text-text-primary">{member.fullName}</h2>
                  <Link to={`/yonetim/uyeler/${member.id}`} className="text-xs text-text-muted hover:text-primary">
                    Üye sayfası →
                  </Link>
                </div>
                <nav aria-label="Program türü" className="inline-flex gap-1 rounded-full border border-border bg-canvas p-1">
                  <NavLink to={`antrenman?uye=${member.id}`} className={tabClass}>
                    Antrenman
                  </NavLink>
                  <NavLink to={`beslenme?uye=${member.id}`} className={tabClass}>
                    Beslenme
                  </NavLink>
                </nav>
              </div>
              <Outlet context={{ member }} />
            </>
          ) : (
            <p className="rounded-2xl border border-dashed border-border-strong p-10 text-center text-sm text-text-muted">
              {selectedId ? 'Yükleniyor…' : 'Soldan bir öğrenci seçin.'}
            </p>
          )}
        </section>
      </div>
    </>
  )
}
