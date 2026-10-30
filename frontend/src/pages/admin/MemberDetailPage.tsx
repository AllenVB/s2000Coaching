import { Link, NavLink, Outlet, useOutletContext, useParams } from 'react-router-dom'

import { useMember } from '@/api/adminMembersApi'
import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { ArrowLeftIcon } from '@/components/ui/icons'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDate } from '@/lib/date'
import type { Member } from '@/types/member'

interface MemberDetailContext {
  member: Member
}

/** Child tabs read the loaded member from here instead of each refetching it. */
export function useMemberDetail(): MemberDetailContext {
  return useOutletContext<MemberDetailContext>()
}

const tabs = [
  { to: '.', label: 'Profil', end: true },
  { to: 'antrenman', label: 'Antrenman' },
  { to: 'beslenme', label: 'Beslenme' },
  { to: 'olcumler', label: 'Ölçümler' },
  { to: 'haftalik', label: 'Haftalık' },
  { to: 'analiz', label: 'Analiz' },
  { to: 'form-kontrol', label: 'Form' },
  { to: 'fotograflar', label: 'Fotoğraflar' },
  { to: 'mesajlar', label: 'Mesajlar' },
  { to: 'notlar', label: 'Notlar' },
]

/** Everything the coach does for one member happens under this page. */
export function MemberDetailPage() {
  const { memberId } = useParams<{ memberId: string }>()
  const { data: member, isLoading, isError, error } = useMember(memberId)
  useSignOutOn401(error)

  return (
    <>
      <Link
        to="/yonetim/uyeler"
        className="mb-4 inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-text-muted transition-colors hover:text-text-primary"
      >
        <ArrowLeftIcon className="h-3.5 w-3.5" />
        Üyeler
      </Link>

      {isLoading ? (
        <div className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Üye yüklenemedi: {error.message}
        </p>
      ) : member ? (
        <>
          <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-3">
                <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary sm:text-3xl">
                  {member.fullName}
                </h1>
                <MemberStatusBadge status={member.status} />
                <Link
                  to="cikti"
                  className="rounded-lg border border-border-strong px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-primary/60 hover:text-text-primary"
                >
                  Plan Çıktısı
                </Link>
              </div>
              <p className="mt-2 break-all text-sm text-text-secondary">{member.email}</p>
              <p className="mt-0.5 text-xs text-text-muted">
                {member.phone} · Kayıt: {formatDate(member.createdAt)}
                {member.verifiedAt ? ` · Onay: ${formatDate(member.verifiedAt)}` : ''}
              </p>
            </div>
          </div>

          <div className="mb-6 flex flex-wrap gap-1.5 border-b border-border pb-3">
            {tabs.map((tab) => (
              <NavLink
                key={tab.label}
                to={tab.to}
                end={tab.end}
                className={({ isActive }) =>
                  `rounded-lg px-3.5 py-2 text-xs font-semibold uppercase tracking-wide transition-colors ${
                    isActive
                      ? 'bg-primary text-canvas'
                      : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
                  }`
                }
              >
                {tab.label}
              </NavLink>
            ))}
          </div>

          <Outlet context={{ member } satisfies MemberDetailContext} />
        </>
      ) : null}
    </>
  )
}
