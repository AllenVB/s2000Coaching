import { useState } from 'react'

import { useCoachLogin } from '@/api/coachAuthApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { ShieldIcon } from '@/components/ui/icons'

/** Management sign-in: a real account, not a shared key. */
export function AdminTokenGate() {
  const login = useCoachLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = email.trim()
    if (trimmed.length > 0 && password.length > 0) {
      login.mutate({ email: trimmed, password })
    }
  }

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="rounded-2xl border border-border bg-surface p-6 sm:p-8">
        <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary">
          Yönetim Girişi
        </h1>
        <p className="mt-2 text-sm leading-relaxed text-text-secondary">
          Koç hesabınızla giriş yapın.
        </p>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <Field label="E-Posta">
            <input
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              type="email"
              autoComplete="username"
              placeholder="koc@s2000coaching.com"
              className={inputClasses}
            />
          </Field>
          <Field label="Şifre">
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              className={inputClasses}
            />
          </Field>

          {login.isError ? <p className="text-xs text-error">{login.error.message}</p> : null}

          <Button
            type="submit"
            className="w-full"
            disabled={login.isPending || email.trim().length === 0 || password.length === 0}
          >
            {login.isPending ? 'Giriş yapılıyor…' : 'Giriş Yap'}
          </Button>
        </form>

        <div className="mt-6 flex items-start gap-2 border-t border-border pt-5 text-[11px] leading-relaxed text-text-muted">
          <ShieldIcon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <span>
            Oturum yalnızca bu sekmede saklanır ve sekmeyi kapattığınızda sona erer. Arka arkaya beş hatalı
            şifreden sonra hesap bir süre kilitlenir.
          </span>
        </div>
      </div>
    </div>
  )
}
