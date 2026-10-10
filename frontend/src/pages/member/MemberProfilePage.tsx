import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'

export function MemberProfilePage() {
  const { member } = useMemberProfileSync()

  if (!member) {
    return null
  }

  return (
    <>
      <PageHeading
        title="Profilim"
        description="Bilgilerinizde bir hata varsa koçunuza bildirin; sizin adınıza güncellenecektir."
      />

      <section className="rounded-2xl border border-border bg-surface p-6">
        <div className="mb-4 flex items-center justify-between gap-3 border-b border-border pb-3">
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Üyelik</h2>
          <MemberStatusBadge status={member.status} />
        </div>

        <dl className="space-y-3">
          <ProfileRow label="Ad Soyad" value={member.fullName} />
          <ProfileRow label="E-Posta" value={member.email} />
          <ProfileRow label="Telefon" value={member.phone} />
          <ProfileRow label="Boy" value={`${member.heightCm} cm`} />
          <ProfileRow label="Başlangıç Kilosu" value={`${member.weightKg} kg`} />
          <ProfileRow label="Üyelik Başlangıcı" value={formatDate(member.createdAt)} />
        </dl>

        <div className="mt-5 border-t border-border pt-4">
          <dt className="mb-1.5 text-[11px] font-semibold uppercase tracking-wider text-text-muted">
            Spor Geçmişi
          </dt>
          <dd className="whitespace-pre-line text-sm leading-relaxed text-text-secondary">
            {member.sportsBackground}
          </dd>
        </div>
      </section>
    </>
  )
}

function ProfileRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className="break-all text-right text-sm text-text-primary">{value}</dd>
    </div>
  )
}
