import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import {
  useMemberLogin,
  useRegisterMember,
  useRequestMemberCode,
  useResetMemberPassword,
  useVerifyMemberCode,
} from '@/api/memberAuthApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { CheckIcon, ShieldIcon } from '@/components/ui/icons'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { ApiError } from '@/lib/api'
import {
  loginSchema,
  memberCodeSchema,
  memberEmailSchema,
  registerSchema,
  resetPasswordSchema,
  type LoginFormValues,
  type MemberCodeFormValues,
  type MemberEmailFormValues,
  type RegisterFormValues,
  type ResetPasswordFormValues,
} from '@/schemas/memberSchema'
import { GENDERS } from '@/types/member'

type Step =
  | { name: 'login' }
  | { name: 'register' }
  /** The account exists but the address is unconfirmed: enter the mailed code. */
  | { name: 'verify'; email: string }
  /** Forgotten password, or a member the coach created who has none yet. */
  | { name: 'forgot' }
  | { name: 'reset'; email: string }

/**
 * The member page's front door: sign in with e-mail and password, or open an account. A forgotten
 * password — and the first sign-in of a member the coach created, who has none — goes through the
 * e-mailed code, which also confirms a new account's address.
 */
export function MemberLogin() {
  const [step, setStep] = useState<Step>({ name: 'login' })
  const tab = step.name === 'register' || step.name === 'verify' ? 'register' : 'login'

  return (
    <div className="mx-auto w-full max-w-md">
      <div className="rounded-2xl border border-border bg-surface p-6 sm:p-8">
        {step.name === 'login' || step.name === 'register' ? (
          <div className="mb-6 grid grid-cols-2 rounded-full border border-border bg-canvas p-1 text-xs font-semibold uppercase tracking-wide">
            <TabButton active={tab === 'login'} onClick={() => setStep({ name: 'login' })}>
              Giriş Yap
            </TabButton>
            <TabButton active={tab === 'register'} onClick={() => setStep({ name: 'register' })}>
              Hesap Oluştur
            </TabButton>
          </div>
        ) : null}

        {step.name === 'login' ? (
          <LoginForm
            onForgot={() => setStep({ name: 'forgot' })}
            onNeedsVerification={(email) => setStep({ name: 'verify', email })}
          />
        ) : null}
        {step.name === 'register' ? (
          <RegisterForm
            onNeedsVerification={(email) => setStep({ name: 'verify', email })}
            onBack={() => setStep({ name: 'login' })}
          />
        ) : null}
        {step.name === 'verify' ? (
          <VerifyEmailForm email={step.email} onBack={() => setStep({ name: 'login' })} />
        ) : null}
        {step.name === 'forgot' ? (
          <ForgotForm onCodeSent={(email) => setStep({ name: 'reset', email })} onBack={() => setStep({ name: 'login' })} />
        ) : null}
        {step.name === 'reset' ? (
          <ResetForm email={step.email} onBack={() => setStep({ name: 'forgot' })} />
        ) : null}

        <div className="mt-6 flex items-start gap-2 border-t border-border pt-5 text-[11px] leading-relaxed text-text-muted">
          <ShieldIcon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <span>
            Şifreniz yalnızca özeti alınarak saklanır; koçunuz bile göremez. Kilo, boy ve ölçümler
            sağlık verisidir: yalnızca siz ve koçunuz erişir.
          </span>
        </div>
      </div>
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`rounded-full px-3 py-2.5 transition-colors ${
        active ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'
      }`}
    >
      {children}
    </button>
  )
}

function Heading({ title, text }: { title: string; text: string }) {
  return (
    <>
      <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary">{title}</h1>
      <p className="mt-2 text-sm leading-relaxed text-text-secondary">{text}</p>
    </>
  )
}

function LinkButton({ onClick, children, disabled }: { onClick: () => void; children: React.ReactNode; disabled?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="font-medium text-primary hover:underline disabled:opacity-50"
    >
      {children}
    </button>
  )
}

/* ---------- sign in ---------- */

function LoginForm({
  onForgot,
  onNeedsVerification,
}: {
  onForgot: () => void
  onNeedsVerification: (email: string) => void
}) {
  const { signIn } = useMemberAuth()
  const login = useMemberLogin()
  const requestCode = useRequestMemberCode()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema), defaultValues: { email: '', password: '' } })

  const onSubmit = (values: LoginFormValues) => {
    login.mutate(values, {
      onSuccess: (session) => signIn(session),
      onError: (error) => {
        // Right password, address never confirmed: send a fresh code and ask for it.
        if (error instanceof ApiError && error.status === 403) {
          requestCode.mutate({ email: values.email })
          onNeedsVerification(values.email)
        }
      },
    })
  }

  return (
    <>
      <Heading title="Üye Girişi" text="E-posta adresiniz ve şifrenizle giriş yapın." />
      <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
        <Field label="E-Posta Adresi" error={errors.email?.message}>
          <input {...register('email')} type="email" autoComplete="email" className={inputClasses} />
        </Field>
        <Field label="Şifre" error={errors.password?.message}>
          <input {...register('password')} type="password" autoComplete="current-password" className={inputClasses} />
        </Field>

        {login.isError && !(login.error instanceof ApiError && login.error.status === 403) ? (
          <p role="alert" className="text-xs text-error">
            {login.error.message}
          </p>
        ) : null}

        <Button type="submit" className="w-full" disabled={login.isPending}>
          {login.isPending ? 'Giriş yapılıyor…' : 'Giriş Yap'}
        </Button>

        <div className="space-y-1.5 text-[11px] leading-relaxed text-text-muted">
          <p>
            <LinkButton onClick={onForgot}>Şifremi unuttum</LinkButton>
          </p>
          <p>
            Koçunuz sizin için hesap açtıysa ve henüz şifreniz yoksa aynı bağlantıdan e-postanıza gelen kodla
            şifre belirleyin.
          </p>
        </div>
      </form>
    </>
  )
}

/* ---------- sign up ---------- */

function RegisterForm({
  onNeedsVerification,
  onBack,
}: {
  onNeedsVerification: (email: string) => void
  onBack: () => void
}) {
  const { signIn } = useMemberAuth()
  const registerMember = useRegisterMember()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { sportsBackground: '', password: '', passwordRepeat: '' } as Partial<RegisterFormValues> as RegisterFormValues,
  })

  const onSubmit = ({ passwordRepeat: _repeat, ...values }: RegisterFormValues) => {
    registerMember.mutate(values, {
      onSuccess: (response) => {
        if (response.verificationRequired || !response.session) {
          onNeedsVerification(values.email)
        } else {
          signIn(response.session)
        }
      },
    })
  }

  return (
    <>
      <Heading
        title="Hesap Oluştur"
        text="Bilgilerinizi girin; koçunuz planınızı bu bilgilere göre hazırlar."
      />
      <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Ad *" error={errors.firstName?.message}>
            <input {...register('firstName')} autoComplete="given-name" className={inputClasses} />
          </Field>
          <Field label="Soyad *" error={errors.lastName?.message}>
            <input {...register('lastName')} autoComplete="family-name" className={inputClasses} />
          </Field>
        </div>

        <Field label="E-Posta *" error={errors.email?.message}>
          <input {...register('email')} type="email" autoComplete="email" className={inputClasses} />
        </Field>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Yaş *" error={errors.age?.message}>
            <input
              {...register('age', { valueAsNumber: true })}
              type="number"
              inputMode="numeric"
              className={inputClasses}
            />
          </Field>
          <Field label="Cinsiyet *" error={errors.gender?.message}>
            <select {...register('gender')} defaultValue="" className={inputClasses}>
              <option value="" disabled>
                Seçin…
              </option>
              {GENDERS.map((gender) => (
                <option key={gender.value} value={gender.value}>
                  {gender.label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Boy (cm) *" error={errors.heightCm?.message}>
            <input
              {...register('heightCm', { valueAsNumber: true })}
              type="number"
              inputMode="numeric"
              className={inputClasses}
            />
          </Field>
          <Field label="Kilo (kg) *" error={errors.weightKg?.message}>
            <input
              {...register('weightKg', { valueAsNumber: true })}
              type="number"
              inputMode="decimal"
              step="0.1"
              className={inputClasses}
            />
          </Field>
        </div>

        <Field label="Spor Geçmişi *" error={errors.sportsBackground?.message}>
          <textarea
            {...register('sportsBackground')}
            rows={3}
            placeholder="Ne kadar süredir, hangi sporları yapıyorsunuz? Yoksa “Yok” yazın."
            className={textareaClasses}
          />
        </Field>

        <div className="grid grid-cols-2 gap-3">
          <Field label="Şifre *" error={errors.password?.message} hint="En az 8 karakter.">
            <input {...register('password')} type="password" autoComplete="new-password" className={inputClasses} />
          </Field>
          <Field label="Şifre Tekrar *" error={errors.passwordRepeat?.message}>
            <input {...register('passwordRepeat')} type="password" autoComplete="new-password" className={inputClasses} />
          </Field>
        </div>

        <div>
          <label className="flex cursor-pointer items-start gap-2.5 text-xs leading-relaxed text-text-secondary">
            <input {...register('acceptedTerms')} type="checkbox" className="mt-0.5" />
            <span>
              Kişisel verilerimin (kilo ve boy gibi sağlık verileri dahil) koçluk hizmetinin sunulması için
              işlenmesini kabul ediyorum.
            </span>
          </label>
          {errors.acceptedTerms ? <p className="mt-1 text-[11px] text-error">{errors.acceptedTerms.message}</p> : null}
        </div>

        {registerMember.isError ? (
          <p role="alert" className="text-xs text-error">
            {registerMember.error.message}
          </p>
        ) : null}

        <Button type="submit" className="w-full" disabled={registerMember.isPending}>
          {registerMember.isPending ? 'Hesap oluşturuluyor…' : 'Hesap Oluştur'}
        </Button>
        <p className="text-center text-[11px] text-text-muted">
          Zaten hesabınız var mı? <LinkButton onClick={onBack}>Giriş yapın</LinkButton>
        </p>
      </form>
    </>
  )
}

/* ---------- confirm the address ---------- */

function CodeSentNote({ email }: { email: string }) {
  return (
    <div className="mt-6 flex items-start gap-2 rounded-lg border border-border bg-surface-elevated p-3.5 text-xs leading-relaxed text-text-secondary">
      <CheckIcon className="mt-0.5 h-4 w-4 shrink-0 text-success" />
      <span>
        <strong className="text-text-primary">{email}</strong> adresine 6 haneli bir kod gönderdik. Kod 15 dakika
        geçerlidir; gelmediyse spam klasörüne de bakın.
      </span>
    </div>
  )
}

function VerifyEmailForm({ email, onBack }: { email: string; onBack: () => void }) {
  const { signIn } = useMemberAuth()
  const verifyCode = useVerifyMemberCode()
  const requestCode = useRequestMemberCode()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MemberCodeFormValues>({ resolver: zodResolver(memberCodeSchema), defaultValues: { code: '' } })

  return (
    <>
      <Heading title="E-postanızı Doğrulayın" text="Hesabınız oluşturuldu. Giriş yapmak için adresinizi doğrulayın." />
      <CodeSentNote email={email} />
      <form
        className="mt-5 space-y-4"
        onSubmit={handleSubmit(({ code }) => verifyCode.mutate({ email, code }, { onSuccess: (session) => signIn(session) }))}
      >
        <Field label="Onay Kodu" error={errors.code?.message}>
          <input
            {...register('code')}
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            className={`${inputClasses} tracking-[0.4em]`}
          />
        </Field>
        {verifyCode.isError ? <p role="alert" className="text-xs text-error">{verifyCode.error.message}</p> : null}
        {requestCode.isError ? <p className="text-xs text-error">{requestCode.error.message}</p> : null}
        {requestCode.isSuccess ? <p className="text-[11px] text-success">Yeni kod gönderildi.</p> : null}
        <Button type="submit" className="w-full" disabled={verifyCode.isPending}>
          {verifyCode.isPending ? 'Doğrulanıyor…' : 'Doğrula ve Giriş Yap'}
        </Button>
        <div className="flex items-center justify-between text-[11px]">
          <LinkButton onClick={() => requestCode.mutate({ email })} disabled={requestCode.isPending}>
            {requestCode.isPending ? 'Gönderiliyor…' : 'Kodu tekrar gönder'}
          </LinkButton>
          <button type="button" onClick={onBack} className="font-medium text-text-muted hover:text-text-primary">
            Giriş ekranına dön
          </button>
        </div>
      </form>
    </>
  )
}

/* ---------- forgotten password ---------- */

function ForgotForm({ onCodeSent, onBack }: { onCodeSent: (email: string) => void; onBack: () => void }) {
  const requestCode = useRequestMemberCode()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MemberEmailFormValues>({ resolver: zodResolver(memberEmailSchema), defaultValues: { email: '' } })

  return (
    <>
      <Heading
        title="Şifre Belirle"
        text="E-posta adresinizi girin, size tek kullanımlık bir kod gönderelim; kodla yeni şifrenizi seçersiniz."
      />
      <form
        className="mt-6 space-y-4"
        onSubmit={handleSubmit(({ email }) => requestCode.mutate({ email }, { onSuccess: () => onCodeSent(email) }))}
      >
        <Field label="E-Posta Adresi" error={errors.email?.message}>
          <input {...register('email')} type="email" autoComplete="email" className={inputClasses} />
        </Field>
        {requestCode.isError ? <p role="alert" className="text-xs text-error">{requestCode.error.message}</p> : null}
        <Button type="submit" className="w-full" disabled={requestCode.isPending}>
          {requestCode.isPending ? 'Gönderiliyor…' : 'Kod Gönder'}
        </Button>
        <p className="text-center text-[11px]">
          <button type="button" onClick={onBack} className="font-medium text-text-muted hover:text-text-primary">
            Giriş ekranına dön
          </button>
        </p>
      </form>
    </>
  )
}

function ResetForm({ email, onBack }: { email: string; onBack: () => void }) {
  const { signIn } = useMemberAuth()
  const reset = useResetMemberPassword()
  const requestCode = useRequestMemberCode()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { code: '', newPassword: '', newPasswordRepeat: '' },
  })

  return (
    <>
      <Heading title="Yeni Şifre" text="Kodu ve yeni şifrenizi girin; giriş yapılır." />
      <CodeSentNote email={email} />
      <form
        className="mt-5 space-y-4"
        onSubmit={handleSubmit(({ code, newPassword }) =>
          reset.mutate({ email, code, newPassword }, { onSuccess: (session) => signIn(session) }),
        )}
      >
        <Field label="Onay Kodu" error={errors.code?.message}>
          <input
            {...register('code')}
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="000000"
            className={`${inputClasses} tracking-[0.4em]`}
          />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Yeni Şifre *" error={errors.newPassword?.message} hint="En az 8 karakter.">
            <input {...register('newPassword')} type="password" autoComplete="new-password" className={inputClasses} />
          </Field>
          <Field label="Tekrar *" error={errors.newPasswordRepeat?.message}>
            <input {...register('newPasswordRepeat')} type="password" autoComplete="new-password" className={inputClasses} />
          </Field>
        </div>
        {reset.isError ? <p role="alert" className="text-xs text-error">{reset.error.message}</p> : null}
        {requestCode.isSuccess ? <p className="text-[11px] text-success">Yeni kod gönderildi.</p> : null}
        <Button type="submit" className="w-full" disabled={reset.isPending}>
          {reset.isPending ? 'Kaydediliyor…' : 'Şifreyi Kaydet ve Giriş Yap'}
        </Button>
        <div className="flex items-center justify-between text-[11px]">
          <LinkButton onClick={() => requestCode.mutate({ email })} disabled={requestCode.isPending}>
            {requestCode.isPending ? 'Gönderiliyor…' : 'Kodu tekrar gönder'}
          </LinkButton>
          <button type="button" onClick={onBack} className="font-medium text-text-muted hover:text-text-primary">
            E-postayı değiştir
          </button>
        </div>
      </form>
    </>
  )
}
