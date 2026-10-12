import type { MemberStatus } from '@/types/member'

const labels: Record<MemberStatus, string> = {
  PENDING_VERIFICATION: 'Onay bekliyor',
  ACTIVE: 'Aktif',
}

const toneClasses: Record<MemberStatus, string> = {
  PENDING_VERIFICATION: 'border-warning/40 bg-warning/10 text-warning',
  ACTIVE: 'border-success/40 bg-success/10 text-success',
}

export function MemberStatusBadge({ status }: { status: MemberStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide ${toneClasses[status]}`}
    >
      {labels[status]}
    </span>
  )
}
