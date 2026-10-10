import { useEffect } from 'react'

import { useMemberLogout, useMemberProfile } from '@/api/memberAuthApi'
import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { ApiError } from '@/lib/api'
import { formatDate } from '@/lib/date'

export function MemberDashboard() {
  const { session, signOut, updateMember } = useMemberAuth()
  const token = session?.token ?? null
  const profile = useMemberProfile(token)
  const logout = useMemberLogout()

  const fetchedMember = profile.data
  useEffect(() => {
    if (fetchedMember) {
      updateMember(fetchedMember)
    }
  }, [fetchedMember, updateMember])

  // A deleted member or an expired session answers 401 — drop the local session so the
  // sign-in form comes back instead of an endless error.
  const profileError = profile.error
  useEffect(() => {
    if (profileError instanceof ApiError && profileError.status === 401) {
      signOut()
    }
  }, [profileError, signOut])

  if (!session) {
    return null
  }

  const member = fetchedMember ?? session.member

  const handleLogout = () => {
    // Sign out locally regardless: the stored token is useless to us either way.
    logout.mutate({ token: session.token }, { onSettled: () => signOut() })
  }

  return (
    <div className="mx-auto w-full max-w-[1280px] space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary sm:text-3xl">
            Merhaba, {member.fullName}
          </h1>
          <p className="mt-2 text-sm text-text-secondary">
            Üye alanınız. Planlarınız ve ölçümleriniz burada toplanacak.
          </p>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          disabled={logout.isPending}
          className="h-9 rounded-lg border border-border bg-surface px-3.5 text-xs font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-white/25 hover:text-text-primary disabled:opacity-50"
        >
          {logout.isPending ? 'Çıkılıyor…' : 'Çıkış Yap'}
        </button>
      </div>

      {profile.isError && !(profileError instanceof ApiError && profileError.status === 401) ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Profiliniz yenilenemedi: {profile.error.message}
        </p>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-12">
        <section className="rounded-2xl border border-border bg-surface p-6 lg:col-span-5">
          <div className="mb-4 flex items-center justify-between gap-3 border-b border-border pb-3">
            <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Profilim</h2>
            <MemberStatusBadge status={member.status} />
          </div>
          <dl className="space-y-3">
            <ProfileRow label="Ad Soyad" value={member.fullName} />
            <ProfileRow label="E-Posta" value={member.email} />
            <ProfileRow label="Telefon" value={member.phone} />
            <ProfileRow label="Boy" value={`${member.heightCm} cm`} />
            <ProfileRow label="Kilo" value={`${member.weightKg} kg`} />
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

        <div className="space-y-6 lg:col-span-7">
          <PlaceholderCard
            title="Antrenman Planım"
            description="Koçunuz antrenman planınızı yüklediğinde bu alanda görünecek."
          />
          <PlaceholderCard
            title="Beslenme Planım"
            description="Makro hedefleriniz ve beslenme planınız bu alanda listelenecek."
          />
          <PlaceholderCard
            title="Ölçüm Takibi"
            description="Haftalık ölçüm ve gelişim grafikleriniz bu alanda yer alacak."
          />
        </div>
      </div>
    </div>
  )
}

function ProfileRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className="text-right text-sm text-text-primary">{value}</dd>
    </div>
  )
}

/** Placeholder for the plan modules that land in the next stage of the member area. */
function PlaceholderCard({ title, description }: { title: string; description: string }) {
  return (
    <section className="rounded-2xl border border-dashed border-border-strong bg-surface/40 p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed text-text-secondary">{description}</p>
      <p className="mt-3 text-[11px] font-semibold uppercase tracking-wider text-text-muted">Yakında</p>
    </section>
  )
}
