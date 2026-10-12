import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { useRequestMemberCode, useVerifyMemberCode } from '@/api/memberAuthApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { CheckIcon, ShieldIcon } from '@/components/ui/icons'
import { useMemberAuth } from '@/context/MemberAuthContext'
import {
  memberCodeSchema,
  memberEmailSchema,
  type MemberCodeFormValues,
  type MemberEmailFormValues,
} from '@/schemas/memberSchema'

/**
 * Passwordless sign-in: ask for a code by e-mail, then exchange it for a session. The same
 * form completes a brand-new membership — the code in the welcome mail activates the account
 * and signs the member in at once.
 */
export function MemberLogin() {
  const { signIn } = useMemberAuth()
  const [email, setEmail] = useState<string | null>(null)
  const requestCode = useRequestMemberCode()
  const verifyCode = useVerifyMemberCode()

  const emailForm = useForm<MemberEmailFormValues>({
    resolver: zodResolver(memberEmailSchema),
    defaultValues: { email: '' },
  })

  const codeForm = useForm<MemberCodeFormValues>({
    resolver: zodResolver(memberCodeSchema),
    defaultValues: { code: '' },
  })

  const onEmailSubmit = ({ email: submitted }: MemberEmailFormValues) => {
    requestCode.mutate(
      { email: submitted },
      {
        onSuccess: () => {
          setEmail(submitted)
          codeForm.reset()
          verifyCode.reset()
        },
      },
    )
  }

  const onCodeSubmit = ({ code }: MemberCodeFormValues) => {
    if (!email) {
      return
    }
    verifyCode.mutate({ email, code }, { onSuccess: (session) => signIn(session) })
  }

  const changeEmail = () => {
    setEmail(null)
    requestCode.reset()
    verifyCode.reset()
  }

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="rounded-2xl border border-border bg-surface p-6 sm:p-8">
        <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary">
          Üye Girişi
        </h1>
        <p className="mt-2 text-sm leading-relaxed text-text-secondary">
          Şifre yok: e-posta adresinizi girin, size tek kullanımlık bir kod gönderelim.
        </p>

        {email === null ? (
          <form className="mt-6 space-y-4" onSubmit={emailForm.handleSubmit(onEmailSubmit)}>
            <Field label="E-Posta Adresi" error={emailForm.formState.errors.email?.message}>
              <input
                {...emailForm.register('email')}
                type="email"
                autoComplete="email"
                placeholder="ornek@s2000coaching.com"
                className={inputClasses}
              />
            </Field>

            {requestCode.isError ? <p className="text-xs text-error">{requestCode.error.message}</p> : null}

            <Button type="submit" className="w-full" disabled={requestCode.isPending}>
              {requestCode.isPending ? 'Gönderiliyor…' : 'Kod Gönder'}
            </Button>
          </form>
        ) : (
          <form className="mt-6 space-y-4" onSubmit={codeForm.handleSubmit(onCodeSubmit)}>
            <div className="flex items-start gap-2 rounded-lg border border-border bg-surface-elevated p-3.5 text-xs leading-relaxed text-text-secondary">
              <CheckIcon className="mt-0.5 h-4 w-4 shrink-0 text-success" />
              <span>
                <strong className="text-text-primary">{email}</strong> adresine 6 haneli bir kod gönderildi.
                Kod 15 dakika geçerlidir.
              </span>
            </div>

            <Field label="Onay Kodu" error={codeForm.formState.errors.code?.message}>
              <input
                {...codeForm.register('code')}
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                placeholder="000000"
                className={`${inputClasses} tracking-[0.4em]`}
              />
            </Field>

            {verifyCode.isError ? <p className="text-xs text-error">{verifyCode.error.message}</p> : null}

            <Button type="submit" className="w-full" disabled={verifyCode.isPending}>
              {verifyCode.isPending ? 'Doğrulanıyor…' : 'Giriş Yap'}
            </Button>

            <div className="flex items-center justify-between text-[11px]">
              <button
                type="button"
                onClick={() => requestCode.mutate({ email })}
                disabled={requestCode.isPending}
                className="font-medium text-primary hover:underline disabled:opacity-50"
              >
                {requestCode.isPending ? 'Gönderiliyor…' : 'Kodu tekrar gönder'}
              </button>
              <button
                type="button"
                onClick={changeEmail}
                className="font-medium text-text-muted hover:text-text-primary"
              >
                E-postayı değiştir
              </button>
            </div>
          </form>
        )}

        <div className="mt-6 flex items-start gap-2 border-t border-border pt-5 text-[11px] leading-relaxed text-text-muted">
          <ShieldIcon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <span>
            Üyelikler koçunuz tarafından oluşturulur. Yeni üye olduysanız hoş geldin e-postanızdaki onay kodunu
            buraya girin — kodu doğruladığınızda üyeliğiniz tamamlanır ve giriş yapmış olursunuz.
          </span>
        </div>
      </div>
    </div>
  )
}
