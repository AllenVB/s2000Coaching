import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { useMembers } from '@/api/adminMembersApi'
import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { Button } from '@/components/ui/Button'
import { inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDate } from '@/lib/date'
import type { MemberStatus } from '@/types/member'

type StatusFilter = 'ALL' | MemberStatus

const filters: { value: StatusFilter; label: string }[] = [
  { value: 'ALL', label: 'Tümü' },
  { value: 'ACTIVE', label: 'Aktif' },
  { value: 'PENDING_VERIFICATION', label: 'Onay bekleyen' },
]

export function MemberListPage() {
  const { data: members, isLoading, isError, error } = useMembers()
  useSignOutOn401(error)

  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<StatusFilter>('ALL')

  const visible = useMemo(() => {
    if (!members) {
      return []
    }
    const term = search.trim().toLocaleLowerCase('tr')
    return members.filter((member) => {
      if (status !== 'ALL' && member.status !== status) {
        return false
      }
      if (term.length === 0) {
        return true
      }
      return (
        member.fullName.toLocaleLowerCase('tr').includes(term) ||
        member.email.toLocaleLowerCase('tr').includes(term) ||
        member.phone.includes(term)
      )
    })
  }, [members, search, status])

  return (
    <>
      <PageHeading
        title="Üyeler"
        description="Bir üyenin planlarını, ölçümlerini ve notlarını düzenlemek için satırına girin."
        action={
          <Link to="/yonetim/uyeler/yeni">
            <Button type="button">Yeni Üye</Button>
          </Link>
        }
      />

      <div className="mb-5 flex flex-wrap items-center gap-3">
        <input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          type="search"
          placeholder="Ad, e-posta veya telefon ara…"
          aria-label="Üye ara"
          className={`${inputClasses} max-w-xs`}
        />
        <div className="flex gap-1.5">
          {filters.map((filter) => (
            <button
              key={filter.value}
              type="button"
              onClick={() => setStatus(filter.value)}
              className={`h-9 rounded-lg border px-3 text-xs font-semibold transition-colors ${
                status === filter.value
                  ? 'border-primary bg-primary/10 text-primary'
                  : 'border-border bg-surface text-text-secondary hover:text-text-primary'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[0, 1, 2].map((index) => (
            <div key={index} className="h-20 animate-pulse rounded-2xl border border-border bg-surface" />
          ))}
        </div>
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Üyeler yüklenemedi: {error.message}
        </p>
      ) : visible.length === 0 ? (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          {members && members.length > 0 ? 'Aramanızla eşleşen üye yok.' : 'Henüz üye eklenmedi.'}
        </p>
      ) : (
        <ul className="space-y-3">
          {visible.map((member) => (
            <li key={member.id}>
              <Link
                to={`/yonetim/uyeler/${member.id}`}
                className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-border bg-surface p-4 transition-colors hover:border-white/20 sm:p-5"
              >
                <span className="min-w-0">
                  <span className="flex flex-wrap items-center gap-2.5">
                    <span className="font-semibold text-text-primary">{member.fullName}</span>
                    <MemberStatusBadge status={member.status} />
                  </span>
                  <span className="mt-1 block break-all text-xs text-text-secondary">{member.email}</span>
                  <span className="mt-0.5 block text-xs text-text-muted">
                    {member.phone} · {member.heightCm} cm · {member.weightKg} kg · Kayıt:{' '}
                    {formatDate(member.createdAt)}
                  </span>
                </span>
                <span className="text-xs font-semibold uppercase tracking-wide text-primary">Detay →</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
