import { Link } from 'react-router-dom'

import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { AppleIcon, ChartIcon, DumbbellIcon } from '@/components/ui/icons'

/** The member's landing screen: what to do today, then the shortcuts into each section. */
export function MemberTodayPage() {
  const { member, error } = useMemberProfileSync()

  if (!member) {
    return null
  }

  return (
    <>
      <PageHeading
        title={`Merhaba, ${member.fullName.split(' ')[0]}`}
        description="Bugünün programı ve kısayolların."
      />

      {error ? (
        <p className="mb-6 rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Bilgileriniz yenilenemedi: {error.message}
        </p>
      ) : null}

      <div className="space-y-6">
        <ComingSoonCard
          title="Bugünün Antrenmanı"
          description="Koçunuz antrenman planınızı yüklediğinde günün programı burada görünecek."
        />

        <div className="grid gap-4 sm:grid-cols-3">
          <ShortcutCard to="/uye/antrenman" label="Antrenman" icon={DumbbellIcon} />
          <ShortcutCard to="/uye/beslenme" label="Beslenme" icon={AppleIcon} />
          <ShortcutCard to="/uye/olcumler" label="Ölçümler" icon={ChartIcon} />
        </div>
      </div>
    </>
  )
}

function ShortcutCard({
  to,
  label,
  icon: Icon,
}: {
  to: string
  label: string
  icon: React.ComponentType<{ className?: string }>
}) {
  return (
    <Link
      to={to}
      className="flex items-center gap-3 rounded-2xl border border-border bg-surface p-5 transition-colors hover:border-white/20"
    >
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
        <Icon className="h-5 w-5" />
      </span>
      <span className="font-semibold text-text-primary">{label}</span>
    </Link>
  )
}
