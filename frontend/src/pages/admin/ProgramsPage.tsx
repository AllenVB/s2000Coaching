import { useMemo, useState } from 'react'
import { Link, NavLink, Outlet, useSearchParams } from 'react-router-dom'

import { useMember, useMembers } from '@/api/adminMembersApi'
import { inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatMemberNumber } from '@/types/member'

/**
 * "Programlar": pick a student (search by name, e-mail, phone or #number), then write their training
 * and nutrition programmes under two tabs on the same page. Each tab is the member's own programme
 * editor — start from a ready template and edit it, or from nothing — so what is done here is exactly
 * what the member's page shows. The chosen student lives in the address (`?uye=`), so a refresh or a
 * link keeps the place.
 */
export function ProgramsPage() {
  const { data: members, isLoading, error } = useMembers()
  useSignOutOn401(error)
  const [params, setParams] = useSearchParams()
  const [search, setSearch] = useState('')
  const selectedId = params.get('uye') ?? undefined
  const { data: member } = useMember(selectedId)

  const visible = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('tr').replace(/^#/, '')
    return (members ?? []).filter(
      (candidate) =>
        term === '' ||
        candidate.fullName.toLocaleLowerCase('tr').includes(term) ||
        candidate.email.toLocaleLowerCase('tr').includes(term) ||
        candidate.phone.includes(term) ||
        String(candidate.memberNumber).includes(term),
    )
  }, [members, search])

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
        <aside className="rounded-2xl border border-border bg-surface p-4 lg:sticky lg:top-24 lg:self-start">
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            type="search"
            placeholder="Öğrenci ara: ad, e-posta, #numara…"
            aria-label="Öğrenci ara"
            className={inputClasses}
          />
          <ul className="mt-3 max-h-[60vh] space-y-1 overflow-y-auto">
            {isLoading ? <li className="p-2 text-xs text-text-muted">Yükleniyor…</li> : null}
            {visible.map((candidate) => (
              <li key={candidate.id}>
                <button
                  type="button"
                  onClick={() => setParams({ uye: candidate.id }, { replace: true })}
                  aria-pressed={candidate.id === selectedId}
                  className={`w-full rounded-xl px-3 py-2.5 text-left transition-colors ${
                    candidate.id === selectedId ? 'bg-primary text-canvas' : 'hover:bg-white/5'
                  }`}
                >
                  <span className="block text-sm font-semibold">{candidate.fullName}</span>
                  <span className={`block text-[11px] ${candidate.id === selectedId ? 'text-canvas/80' : 'text-text-muted'}`}>
                    {formatMemberNumber(candidate.memberNumber)} · {candidate.email}
                  </span>
                </button>
              </li>
            ))}
            {!isLoading && visible.length === 0 ? <li className="p-2 text-xs italic text-text-muted">Eşleşen öğrenci yok.</li> : null}
          </ul>
        </aside>

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
