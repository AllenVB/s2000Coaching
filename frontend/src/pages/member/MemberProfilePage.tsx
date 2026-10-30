import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { useChangeMemberPassword, useSetReminders, useUpdateMyProfile } from '@/api/memberAuthApi'
import { MembershipCard } from '@/components/member/MembershipCard'
import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'
import {
  changePasswordSchema,
  updateMyProfileSchema,
  type ChangePasswordFormValues,
  type UpdateMyProfileFormValues,
} from '@/schemas/memberSchema'
import { genderLabel, type Member } from '@/types/member'

export function MemberProfilePage() {
  const { member } = useMemberProfileSync()
  const [editing, setEditing] = useState(false)

  if (!member) {
    return null
  }

  return (
    <>
      <PageHeading
        title="Profilim"
        description="Yaş, boy, kilo, telefon ve spor geçmişinizi kendiniz güncelleyebilirsiniz."
      />

      <div className="space-y-6">
        <MembershipCard />
        <PasswordCard member={member} />
        <RemindersCard member={member} />
        {editing ? (
          <ProfileForm member={member} onDone={() => setEditing(false)} />
        ) : (
          <ProfileCard member={member} onEdit={() => setEditing(true)} />
        )}
      </div>
    </>
  )
}

function ProfileCard({ member, onEdit }: { member: Member; onEdit: () => void }) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-4 flex items-center justify-between gap-3 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Üyelik</h2>
        <MemberStatusBadge status={member.status} />
      </div>

      <dl className="space-y-3">
        <ProfileRow label="Ad Soyad" value={member.fullName} />
        <ProfileRow label="E-Posta" value={member.email} />
        <ProfileRow label="Cinsiyet" value={genderLabel(member.gender)} />
        <ProfileRow label="Yaş" value={member.age !== null ? String(member.age) : '—'} />
        <ProfileRow label="Telefon" value={member.phone || '—'} />
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

      <Button type="button" variant="secondary" className="mt-5 w-full sm:w-auto" onClick={onEdit}>
        Bilgilerimi Düzenle
      </Button>
    </section>
  )
}

/**
 * What a member owns: age, phone, height, weight and sports background. Name, gender and
 * e-mail are shown read-only — the e-mail identifies the account and receives its codes — and
 * the coach changes them when they need changing.
 */
function ProfileForm({ member, onDone }: { member: Member; onDone: () => void }) {
  const updateProfile = useUpdateMyProfile()

  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<UpdateMyProfileFormValues>({
    resolver: zodResolver(updateMyProfileSchema),
    defaultValues: {
      age: member.age ?? undefined,
      phone: member.phone,
      heightCm: member.heightCm,
      weightKg: member.weightKg,
      sportsBackground: member.sportsBackground,
    },
  })

  const onSubmit = (values: UpdateMyProfileFormValues) => {
    updateProfile.mutate(values, { onSuccess: onDone })
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-5 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          Bilgilerimi Düzenle
        </h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Ad soyad, cinsiyet ve e-posta değiştirilemez; değişmesi gerekiyorsa koçunuza bildirin. Bugünkü
          kilonuzu zamanla izlemek için <span className="text-text-primary">Ölçümler</span> sayfasını kullanın.
        </p>
      </div>

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Ad Soyad">
            <input value={member.fullName} readOnly disabled className={inputClasses} />
          </Field>
          <Field label="Cinsiyet">
            <input value={genderLabel(member.gender)} readOnly disabled className={inputClasses} />
          </Field>
          <Field label="E-Posta">
            <input value={member.email} readOnly disabled className={inputClasses} />
          </Field>
          <Field label="Telefon" error={errors.phone?.message}>
            <input
              {...register('phone')}
              type="tel"
              autoComplete="tel"
              placeholder="+90 555 111 22 33"
              className={inputClasses}
            />
          </Field>

          <Field label="Yaş *" error={errors.age?.message}>
            <input
              {...register('age', { valueAsNumber: true })}
              type="number"
              inputMode="numeric"
              className={inputClasses}
            />
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
          <textarea {...register('sportsBackground')} className={textareaClasses} />
        </Field>

        {updateProfile.isError ? (
          <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
            Bilgileriniz kaydedilemedi: {updateProfile.error.message}
          </p>
        ) : null}

        <div className="flex flex-col gap-2.5 sm:flex-row">
          <Button type="submit" disabled={!isDirty || updateProfile.isPending}>
            {updateProfile.isPending ? 'Kaydediliyor…' : 'Kaydet'}
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={onDone}
            disabled={updateProfile.isPending}
          >
            Vazgeç
          </Button>
        </div>
      </form>
    </section>
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

/** Opt-out for the reminder e-mails; on by default, one click to turn off. */
function RemindersCard({ member }: { member: Member }) {
  const setReminders = useSetReminders()

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-2 text-sm font-bold uppercase tracking-wider text-text-primary">Hatırlatma E-postaları</h2>
      <p className="mb-4 text-xs leading-relaxed text-text-secondary">
        Üyelik bitişi, haftalık değerlendirme ve ölçüm hatırlatmaları e-postanıza gelir. Giriş ve onay kodları
        bundan bağımsızdır, her zaman gönderilir.
      </p>
      <label className="flex cursor-pointer items-center gap-2.5 text-sm text-text-primary">
        <input
          type="checkbox"
          checked={member.remindersEnabled}
          disabled={setReminders.isPending}
          onChange={(event) => setReminders.mutate(event.target.checked)}
        />
        Hatırlatma e-postaları almak istiyorum
      </label>
      {setReminders.isError ? <p className="mt-2 text-xs text-error">{setReminders.error.message}</p> : null}
    </section>
  )
}

/**
 * Sets a password for a member who signs in with e-mailed codes, or changes the existing one
 * (which asks for the current one first). Changing it signs the other devices out.
 */
function PasswordCard({ member }: { member: Member }) {
  const changePassword = useChangeMemberPassword()
  const [done, setDone] = useState(false)
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', newPasswordRepeat: '' },
  })

  const onSubmit = ({ currentPassword, newPassword }: ChangePasswordFormValues) => {
    setDone(false)
    changePassword.mutate(
      { currentPassword, newPassword },
      {
        onSuccess: () => {
          reset()
          setDone(true)
        },
      },
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-2 text-sm font-bold uppercase tracking-wider text-text-primary">
        {member.hasPassword ? 'Şifreyi Değiştir' : 'Şifre Belirle'}
      </h2>
      <p className="mb-4 text-xs leading-relaxed text-text-secondary">
        {member.hasPassword
          ? 'Şifrenizi değiştirdiğinizde diğer cihazlardaki oturumlarınız kapanır.'
          : 'Henüz şifreniz yok; e-posta koduyla giriş yapıyorsunuz. Bir şifre belirlerseniz bundan sonra e-posta ve şifrenizle girebilirsiniz.'}
      </p>
      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
        {member.hasPassword ? (
          <Field label="Mevcut Şifre" error={errors.currentPassword?.message}>
            <input {...register('currentPassword')} type="password" autoComplete="current-password" className={inputClasses} />
          </Field>
        ) : null}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Yeni Şifre *" error={errors.newPassword?.message} hint="En az 8 karakter.">
            <input {...register('newPassword')} type="password" autoComplete="new-password" className={inputClasses} />
          </Field>
          <Field label="Yeni Şifre Tekrar *" error={errors.newPasswordRepeat?.message}>
            <input {...register('newPasswordRepeat')} type="password" autoComplete="new-password" className={inputClasses} />
          </Field>
        </div>
        {changePassword.isError ? <p role="alert" className="text-xs text-error">{changePassword.error.message}</p> : null}
        {done ? <p className="text-xs text-success">Şifreniz kaydedildi.</p> : null}
        <Button type="submit" variant="secondary" disabled={changePassword.isPending}>
          {changePassword.isPending ? 'Kaydediliyor…' : member.hasPassword ? 'Şifreyi Değiştir' : 'Şifreyi Kaydet'}
        </Button>
      </form>
    </section>
  )
}
