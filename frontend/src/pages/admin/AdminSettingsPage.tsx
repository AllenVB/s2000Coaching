import { useState, type FormEvent } from 'react'

import { useChangeCoachPassword } from '@/api/coachAuthApi'
import { PageHeading } from '@/components/ui/PageHeading'
import { Field, inputClasses } from '@/components/ui/FormField'
import { useAdminAuth } from '@/context/AdminAuthContext'
import { useAdminSignOut } from '@/hooks/useAdminSession'
import { Button } from '@/components/ui/Button'

const MIN_PASSWORD_LENGTH = 10

export function AdminSettingsPage() {
  const signOut = useAdminSignOut()

  return (
    <>
      <PageHeading title="Ayarlar" description="Yönetim erişimi ve sistem yapılandırması." />

      <div className="space-y-6">
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Yönetim Erişimi</h2>
          <p className="mt-2 text-xs leading-relaxed text-text-secondary">
            Giriş koç hesabıyla yapılır; şifreler bcrypt ile özetlenerek saklanır. İlk hesap sunucuda{' '}
            <code className="text-text-primary">ADMIN_EMAIL</code> ve{' '}
            <code className="text-text-primary">ADMIN_PASSWORD</code> ile oluşturulur; mevcut bir hesap asla
            üzerine yazılmaz. Oturum bu sekmede saklanır ve çıkışta sunucu tarafında da iptal edilir.
          </p>
          <Button type="button" variant="secondary" className="mt-4" onClick={signOut}>
            Oturumu Kapat
          </Button>
        </section>

        <PasswordSection />

        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">E-Posta Gönderimi</h2>
          <p className="mt-2 text-xs leading-relaxed text-text-secondary">
            Onay ve giriş kodları SMTP üzerinden gönderilir. Sunucuda{' '}
            <code className="text-text-primary">MAIL_HOST</code> tanımlı değilse kodlar yalnızca uygulama
            loguna yazılır ve üyelere e-posta ulaşmaz. Aynı adrese kod isteme sıklığı sınırlıdır: iki kod
            arasında en az bir dakika, saatte en fazla beş kod.
          </p>
        </section>
      </div>
    </>
  )
}

function PasswordSection() {
  const { token } = useAdminAuth()
  const signOut = useAdminSignOut()
  const changePassword = useChangeCoachPassword()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [repeatPassword, setRepeatPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    setError(null)

    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      setError(`Yeni şifre en az ${MIN_PASSWORD_LENGTH} karakter olmalı.`)
      return
    }
    if (newPassword !== repeatPassword) {
      setError('Yeni şifre ile tekrarı aynı değil.')
      return
    }
    if (!token) {
      setError('Oturum bulunamadı. Tekrar giriş yapın.')
      return
    }

    changePassword.mutate(
      { token, currentPassword, newPassword },
      {
        onSuccess: () => {
          // The server has dropped every session of this account, so the token in this tab is
          // already dead; showing the panel as if it still worked would mislead.
          setDone(true)
          setCurrentPassword('')
          setNewPassword('')
          setRepeatPassword('')
        },
        onError: (cause) => setError(cause instanceof Error ? cause.message : 'Şifre değiştirilemedi.'),
      },
    )
  }

  if (done) {
    return (
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Şifre Değiştirme</h2>
        <p className="mt-2 text-xs leading-relaxed text-text-secondary">
          Şifre değiştirildi. Güvenlik için bu hesabın tüm oturumları kapatıldı; yeni şifrenle tekrar giriş
          yap.
        </p>
        <Button type="button" className="mt-4" onClick={signOut}>
          Giriş Ekranına Dön
        </Button>
      </section>
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Şifre Değiştirme</h2>
      <p className="mt-2 text-xs leading-relaxed text-text-secondary">
        Şifreni değiştirdiğinde bu hesabın açık olan bütün oturumları kapanır — başka bir cihazda açık
        kalmış bir oturum varsa o da düşer.
      </p>

      <form className="mt-4 max-w-sm space-y-4" onSubmit={submit}>
        <Field label="Mevcut şifre">
          <input
            type="password"
            autoComplete="current-password"
            className={inputClasses}
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            required
          />
        </Field>
        <Field label="Yeni şifre" hint={`En az ${MIN_PASSWORD_LENGTH} karakter.`}>
          <input
            type="password"
            autoComplete="new-password"
            className={inputClasses}
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            required
          />
        </Field>
        <Field label="Yeni şifre (tekrar)" error={error ?? undefined}>
          <input
            type="password"
            autoComplete="new-password"
            className={inputClasses}
            value={repeatPassword}
            onChange={(event) => setRepeatPassword(event.target.value)}
            required
          />
        </Field>

        <Button type="submit" disabled={changePassword.isPending}>
          {changePassword.isPending ? 'Değiştiriliyor…' : 'Şifreyi Değiştir'}
        </Button>
      </form>
    </section>
  )
}
