import { PasswordChecklist } from '@/components/ui/PasswordChecklist'
import { isStrongPassword } from '@/lib/password'
import { formatMemberNumber } from '@/types/member'
import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useLocation, useNavigate } from 'react-router-dom'

import {
  useCompleteRegistration,
  useDeleteMember,
  useResendVerificationCode,
  useSetMemberPassword,
  useUpdateMember,
} from '@/api/adminMembersApi'
import { useMemberOrders } from '@/api/ordersAdminApi'
import { MailDeliveryNotice } from '@/components/admin/MailDeliveryNotice'
import { MemberQuestionnaireCard } from '@/components/admin/MemberQuestionnaireCard'
import { MemberMembershipsCard } from '@/components/admin/MemberMembershipsCard'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { CheckIcon, TrashIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import type { MailDelivery } from '@/types/member'
import { formatMinor } from '@/lib/money'
import { ORDER_STATUS, TONE_CLASSES } from '@/lib/orderStatus'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'
import { updateMemberSchema, type UpdateMemberFormValues } from '@/schemas/memberSchema'
import { GENDERS } from '@/types/member'

export function MemberProfileTab() {
  const { member } = useMemberDetail()
  const navigate = useNavigate()

  const updateMember = useUpdateMember(member.id)
  const deleteMember = useDeleteMember()
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<UpdateMemberFormValues>({
    resolver: zodResolver(updateMemberSchema),
    // `values` (not `defaultValues`) so the form follows the member after a refetch.
    values: {
      email: member.email,
      fullName: member.fullName,
      gender: member.gender ?? '',
      age: member.age !== null ? String(member.age) : '',
      phone: member.phone,
      heightCm: member.heightCm,
      weightKg: member.weightKg,
      sportsBackground: member.sportsBackground,
    },
  })

  return (
    <div className="space-y-6">
      {member.status === 'PENDING_VERIFICATION' ? <PendingVerificationCard memberId={member.id} /> : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-5 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
          Üye Bilgileri
        </h2>
        <p className="mb-4 text-xs text-text-secondary">
          Üye No <span className="font-semibold text-text-primary">{formatMemberNumber(member.memberNumber)}</span>
          {' '}— aynı adı taşıyan üyeleri ayırt etmek için verilir, değiştirilemez.
        </p>

        <form className="space-y-4" onSubmit={handleSubmit(({ gender, age, ...values }) =>
            updateMember.mutate({ ...values, gender: gender === '' ? null : gender, age: age === '' ? null : Number(age) }),
          )}>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Ad Soyad *" error={errors.fullName?.message}>
              <input {...register('fullName')} type="text" className={inputClasses} />
            </Field>
            <Field label="E-Posta *" error={errors.email?.message} hint="Üyenin giriş adresi; başkasında kayıtlı olamaz.">
              <input {...register('email')} type="email" className={inputClasses} />
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Field label="Cinsiyet" error={errors.gender?.message}>
              <select {...register('gender')} className={inputClasses}>
                <option value="">Belirtilmemiş</option>
                {GENDERS.map((gender) => (
                  <option key={gender.value} value={gender.value}>
                    {gender.label}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Yaş" error={errors.age?.message}>
              <input {...register('age')} type="text" inputMode="numeric" className={inputClasses} />
            </Field>
            <Field label="Telefon" error={errors.phone?.message}>
              <input {...register('phone')} type="tel" className={inputClasses} />
            </Field>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Boy (cm) *" error={errors.heightCm?.message}>
              <input
                {...register('heightCm', { valueAsNumber: true })}
                type="number"
                min={100}
                max={250}
                className={inputClasses}
              />
            </Field>
            <Field
              label="Kilo (kg) *"
              error={errors.weightKg?.message}
              hint="Başlangıç kilosu; haftalık değişim Ölçümler sekmesinde."
            >
              <input
                {...register('weightKg', { valueAsNumber: true })}
                type="number"
                min={30}
                max={300}
                step="0.1"
                className={inputClasses}
              />
            </Field>
          </div>

          <Field label="Spor Geçmişi *" error={errors.sportsBackground?.message}>
            <textarea {...register('sportsBackground')} rows={4} className={textareaClasses} />
          </Field>

          {updateMember.isError ? (
            <p className="text-xs text-error">Kaydedilemedi: {updateMember.error.message}</p>
          ) : null}
          {updateMember.isSuccess && !isDirty ? (
            <p className="flex items-center gap-1.5 text-xs text-success">
              <CheckIcon className="h-3.5 w-3.5" /> Kaydedildi.
            </p>
          ) : null}

          <div className="flex flex-wrap gap-2">
            <Button type="submit" disabled={updateMember.isPending || !isDirty}>
              {updateMember.isPending ? 'Kaydediliyor…' : 'Kaydet'}
            </Button>
            <Button type="button" variant="ghost" onClick={() => reset()} disabled={!isDirty}>
              Geri Al
            </Button>
          </div>
        </form>
      </section>

      <SetPasswordCard memberId={member.id} hasPassword={member.hasPassword} />

      <MemberQuestionnaireCard memberId={member.id} />

      <MemberMembershipsCard memberId={member.id} />

      <MemberOrdersCard memberId={member.id} />

      <section className="rounded-2xl border border-error/30 bg-surface p-6">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Üyeliği Sil</h2>
        <p className="mt-2 text-xs leading-relaxed text-text-secondary">
          Üyenin oturumları, bekleyen onay kodları ve koç notları da silinir. Bu işlem geri alınamaz.
        </p>
        <Button type="button" variant="danger" className="mt-4" onClick={() => setConfirmingDelete(true)}>
          <TrashIcon className="h-3.5 w-3.5" />
          Üyeyi Sil
        </Button>
      </section>

      {isConfirmingDelete ? (
        <ConfirmDialog
          title="Üye Silme Onayı"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteMember.isPending}
          error={deleteMember.isError ? deleteMember.error.message : null}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() =>
            deleteMember.mutate({ memberId: member.id }, { onSuccess: () => navigate('/yonetim/uyeler') })
          }
          description={
            <>
              <p className="mb-3">Bu kullanıcıyı silmek istediğinize emin misiniz?</p>
              <p className="rounded-xl border border-border bg-surface-elevated p-3.5 text-xs">
                <strong className="text-text-primary">{member.fullName}</strong>
                <br />
                <span className="break-all text-text-secondary">{member.email}</span>
              </p>
              <p className="mt-3 text-xs text-text-muted">Bu işlem geri alınamaz.</p>
            </>
          }
        />
      ) : null}
    </div>
  )
}

/** Orders placed with this member's address, attached when the member was created. */
function MemberOrdersCard({ memberId }: { memberId: string }) {
  const { data: orders } = useMemberOrders(memberId)

  if (!orders || orders.length === 0) {
    return null
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
        Siparişler
      </h2>
      <ul className="space-y-2">
        {orders.map((order) => (
          <li
            key={order.orderNumber}
            className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3 text-sm"
          >
            <span className="min-w-0">
              <span className="block font-semibold text-text-primary">{order.packageName}</span>
              <span className="block text-xs text-text-muted">
                {formatDate(order.createdAt)} · <span className="font-mono">{order.orderNumber}</span>
              </span>
            </span>
            <span className="flex items-center gap-3">
              <span
                className={`inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] font-semibold ${TONE_CLASSES[ORDER_STATUS[order.status].tone]}`}
              >
                {ORDER_STATUS[order.status].label}
              </span>
              <span className="font-semibold text-primary">{formatMinor(order.priceMinor)}</span>
            </span>
          </li>
        ))}
      </ul>
    </section>
  )
}

/** Shown until the mailed registration code is confirmed. */
function PendingVerificationCard({ memberId }: { memberId: string }) {
  const [code, setCode] = useState('')
  const completeRegistration = useCompleteRegistration()
  const resendCode = useResendVerificationCode()
  // How the first invitation went (passed along when the member was created), until a resend
  // replaces it with a newer answer. Unknown when the page is opened some other way.
  const location = useLocation()
  const firstDelivery = (location.state as { inviteDelivery?: MailDelivery } | null)?.inviteDelivery
  const delivery = resendCode.data?.delivery ?? firstDelivery

  const submitCode = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = code.trim()
    if (trimmed.length === 0) {
      return
    }
    resendCode.reset()
    completeRegistration.mutate({ memberId, code: trimmed }, { onSuccess: () => setCode('') })
  }

  return (
    <section className="rounded-2xl border border-warning/40 bg-warning/5 p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Üyelik Onayı Bekleniyor</h2>
      <p className="mt-2 text-xs leading-relaxed text-text-secondary">
        Üye kodu üye sayfasında kendisi girebilir; kodu size ilettiyse buradan da tamamlayabilirsiniz.
      </p>
      {delivery ? (
        <div className="mt-3">
          <MailDeliveryNotice delivery={delivery} />
        </div>
      ) : null}

      <form className="mt-4 flex flex-wrap items-start gap-2" onSubmit={submitCode}>
        <div>
          <input
            value={code}
            onChange={(event) => setCode(event.target.value)}
            type="text"
            inputMode="numeric"
            maxLength={6}
            placeholder="000000"
            aria-label="Onay kodu"
            className={`${inputClasses} w-32 tracking-[0.3em]`}
          />
          {completeRegistration.isError ? (
            <p className="mt-1 text-[11px] text-error">{completeRegistration.error.message}</p>
          ) : null}
        </div>
        <Button type="submit" disabled={completeRegistration.isPending}>
          {completeRegistration.isPending ? 'Doğrulanıyor…' : 'Üyeliği Tamamla'}
        </Button>
        <Button
          type="button"
          variant="ghost"
          onClick={() => {
            completeRegistration.reset()
            resendCode.mutate({ memberId })
          }}
          disabled={resendCode.isPending}
        >
          {resendCode.isPending ? 'Gönderiliyor…' : 'Kodu Tekrar Gönder'}
        </Button>
      </form>

      {resendCode.isError ? <p className="mt-2 text-[11px] text-error">{resendCode.error.message}</p> : null}
    </section>
  )
}

/**
 * The coach choosing a password for a member who forgot theirs or has none. It is never shown
 * back — only replaced — and the member's current sessions end when it is set.
 */
function SetPasswordCard({ memberId, hasPassword }: { memberId: string; hasPassword: boolean }) {
  const setPassword = useSetMemberPassword(memberId)
  const [value, setValue] = useState('')
  const weak = value.length > 0 && !isStrongPassword(value)

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Şifre</h2>
      <p className="mt-2 text-xs leading-relaxed text-text-secondary">
        {hasPassword ? 'Üyenin şifresi var; göremezsiniz, yalnızca yenisini belirleyebilirsiniz.' : 'Üyenin henüz şifresi yok; e-posta koduyla giriyor.'}{' '}
        Belirlediğinizde üyenin açık oturumları kapanır; yeni şifreyi ona kendiniz iletin.
      </p>
      <form
        className="mt-4 flex flex-wrap items-start gap-2"
        onSubmit={(event) => {
          event.preventDefault()
          if (isStrongPassword(value)) {
            setPassword.mutate(value, { onSuccess: () => setValue('') })
          }
        }}
      >
        <div>
          <input
            value={value}
            onChange={(event) => setValue(event.target.value)}
            type="text"
            autoComplete="off"
            maxLength={64}
            placeholder="Yeni şifre"
            aria-label="Yeni şifre"
            className={`${inputClasses} w-64`}
          />
          {weak || value.length === 0 ? <div className="mt-2"><PasswordChecklist value={value} /></div> : null}
        </div>
        <Button type="submit" variant="secondary" disabled={setPassword.isPending || !isStrongPassword(value)}>
          {setPassword.isPending ? 'Kaydediliyor…' : 'Şifreyi Belirle'}
        </Button>
      </form>
      {setPassword.isError ? <p className="mt-2 text-xs text-error">{setPassword.error.message}</p> : null}
      {setPassword.isSuccess ? <p className="mt-2 text-xs text-success">Şifre belirlendi; üyenin oturumları kapandı.</p> : null}
    </section>
  )
}
