import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'

import { fetchMembersWithToken } from '@/api/adminMembersApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { ShieldIcon } from '@/components/ui/icons'
import { useAdminAuth } from '@/context/AdminAuthContext'

/**
 * Asks for the management key and keeps it only after the API accepts it, so a typo is
 * caught here instead of surfacing as a failure on every later request.
 */
export function AdminTokenGate() {
  const { signIn } = useAdminAuth()
  const [token, setToken] = useState('')

  const verify = useMutation({
    mutationFn: (candidate: string) => fetchMembersWithToken(candidate),
    onSuccess: (_members, candidate) => signIn(candidate),
  })

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = token.trim()
    if (trimmed.length > 0) {
      verify.mutate(trimmed)
    }
  }

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="rounded-2xl border border-border bg-surface p-6 sm:p-8">
        <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary">
          Yönetim Girişi
        </h1>
        <p className="mt-2 text-sm leading-relaxed text-text-secondary">
          Üye yönetimi için yönetim anahtarınızı girin.
        </p>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
          <Field label="Yönetim Anahtarı">
            <input
              value={token}
              onChange={(event) => setToken(event.target.value)}
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              className={inputClasses}
            />
          </Field>

          {verify.isError ? <p className="text-xs text-error">{verify.error.message}</p> : null}

          <Button type="submit" className="w-full" disabled={verify.isPending || token.trim().length === 0}>
            {verify.isPending ? 'Doğrulanıyor…' : 'Giriş Yap'}
          </Button>
        </form>

        <div className="mt-6 flex items-start gap-2 border-t border-border pt-5 text-[11px] leading-relaxed text-text-muted">
          <ShieldIcon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <span>
            Anahtar yalnızca bu sekmede saklanır (sessionStorage); sekmeyi kapattığınızda yönetim erişimi sona
            erer. Anahtar sunucuda <code className="text-text-secondary">ADMIN_TOKEN</code> ile tanımlanır.
          </span>
        </div>
      </div>
    </div>
  )
}
