import { Link } from 'react-router-dom'

import { useAdminDashboard } from '@/api/adminMembersApi'
import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDate } from '@/lib/date'
import { formatMinor } from '@/lib/money'
import type { Member } from '@/types/member'

/** Opens on the only question that matters first thing: who needs attention today. */
export function AdminDashboardPage() {
  const { data, isLoading, isError, error } = useAdminDashboard()
  useSignOutOn401(error)

  return (
    <>
      <PageHeading
        title="Panel"
        description="Onay bekleyenler ve son siparişler. Üyelerin tamamı için Üyeler sekmesine geçin."
      />

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2">
          {[0, 1].map((index) => (
            <div key={index} className="h-28 animate-pulse rounded-2xl border border-border bg-surface" />
          ))}
        </div>
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Panel yüklenemedi: {error.message}
        </p>
      ) : data ? (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-3">
            <StatCard label="Toplam Üye" value={String(data.totalMembers)} />
            <StatCard label="Aktif Üye" value={String(data.activeMembers)} />
            <StatCard
              label="Onay Bekleyen"
              value={String(data.pendingMembers.length)}
              tone={data.pendingMembers.length > 0 ? 'warning' : 'default'}
            />
          </div>

          <MemberCallout
            title="Onay Bekleyen Üyeler"
            emptyLabel="Onay bekleyen üye yok."
            members={data.pendingMembers}
            showStatus
          />

          <MemberCallout
            title={`${data.staleAfterDays} Gündür Ölçüm Girmeyenler`}
            emptyLabel="Takipte gecikme yok."
            members={data.staleMembers}
          />

          <section className="rounded-2xl border border-border bg-surface p-6">
            <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
              Son Siparişler
            </h2>
            {data.recentOrders.length === 0 ? (
              <p className="text-sm italic text-text-muted">Henüz sipariş yok.</p>
            ) : (
              <ul className="space-y-2">
                {data.recentOrders.slice(0, 5).map((order) => (
                  <li
                    key={order.orderNumber}
                    className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3"
                  >
                    <span className="min-w-0">
                      <span className="block font-semibold text-text-primary">{order.customerName}</span>
                      <span className="block break-all text-xs text-text-secondary">
                        {order.packageName} · {formatDate(order.createdAt)}
                      </span>
                    </span>
                    <span className="text-sm font-semibold text-primary">{formatMinor(order.priceMinor)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      ) : null}
    </>
  )
}

/** A short "these people need you" list; the whole row links into the member's page. */
function MemberCallout({
  title,
  emptyLabel,
  members,
  showStatus = false,
}: {
  title: string
  emptyLabel: string
  members: Member[]
  showStatus?: boolean
}) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
        {title}
      </h2>
      {members.length === 0 ? (
        <p className="text-sm italic text-text-muted">{emptyLabel}</p>
      ) : (
        <ul className="space-y-2">
          {members.map((member) => (
            <li key={member.id}>
              <Link
                to={`/yonetim/uyeler/${member.id}`}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3 transition-colors hover:border-white/20"
              >
                <span className="min-w-0">
                  <span className="block font-semibold text-text-primary">{member.fullName}</span>
                  <span className="block break-all text-xs text-text-secondary">{member.email}</span>
                </span>
                {showStatus ? <MemberStatusBadge status={member.status} /> : null}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function StatCard({
  label,
  value,
  tone = 'default',
}: {
  label: string
  value: string
  tone?: 'default' | 'warning'
}) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <span className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</span>
      <p
        className={`mt-1.5 font-display text-3xl font-extrabold ${
          tone === 'warning' ? 'text-warning' : 'text-text-primary'
        }`}
      >
        {value}
      </p>
    </div>
  )
}
