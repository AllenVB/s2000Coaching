import { useMembers } from '@/api/adminMembersApi'
import { MemberRow } from '@/components/admin/MemberRow'

export function MemberList() {
  const { data: members, isLoading, isError, error } = useMembers()

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Üyeler</h2>
        {members ? (
          <span className="text-xs text-text-muted">
            {members.length} üye · {members.filter((member) => member.status === 'ACTIVE').length} aktif
          </span>
        ) : null}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {[0, 1].map((index) => (
            <div key={index} className="h-24 animate-pulse rounded-2xl border border-border bg-surface-elevated" />
          ))}
        </div>
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Üyeler yüklenemedi: {error.message}
        </p>
      ) : members && members.length > 0 ? (
        <ul className="space-y-3">
          {members.map((member) => (
            <MemberRow key={member.id} member={member} />
          ))}
        </ul>
      ) : (
        <p className="text-sm italic text-text-muted">Henüz üye eklenmedi.</p>
      )}
    </section>
  )
}
