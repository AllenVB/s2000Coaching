import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { useSetReminders, useUpdateMyProfile } from '@/api/memberAuthApi'
import { MembershipCard } from '@/components/member/MembershipCard'
import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'
import { updateMyProfileSchema, type UpdateMyProfileFormValues } from '@/schemas/memberSchema'
import type { Member } from '@/types/member'

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
        description="İletişim bilgilerinizi kendiniz güncelleyebilirsiniz."
      />

      <div className="space-y-6">
        <MembershipCard />
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

      <Button type="button" variant="secondary" className="mt-5 w-full sm:w-auto" onClick={onEdit}>
        Bilgilerimi Düzenle
      </Button>
    </section>
  )
}

/**
 * Only the fields a member owns. The e-mail receives their login codes and the starting
 * weight is what their progress is measured from, so both are shown as read-only here and
 * changed by the coach instead.
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
      fullName: member.fullName,
      phone: member.phone,
      heightCm: member.heightCm,
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
          E-posta adresiniz giriş kodlarınızın gittiği yer, başlangıç kilonuz da gelişiminizin
          ölçüldüğü nokta — ikisini de koçunuz değiştirebilir. Bugünkü kilonuzu{' '}
          <span className="text-text-primary">Ölçümler</span> sayfasından girin.
        </p>
      </div>

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Ad Soyad *" error={errors.fullName?.message}>
            <input {...register('fullName')} autoComplete="name" className={inputClasses} />
          </Field>

          <Field label="Telefon *" error={errors.phone?.message}>
            <input
              {...register('phone')}
              type="tel"
              autoComplete="tel"
              placeholder="+90 555 111 22 33"
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

          <Field label="E-Posta" hint="Değiştirmek için koçunuza bildirin.">
            <input value={member.email} readOnly disabled className={inputClasses} />
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
