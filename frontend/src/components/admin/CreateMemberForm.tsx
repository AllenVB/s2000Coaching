import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'

import { useCreateMember } from '@/api/adminMembersApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { createMemberSchema, type CreateMemberFormValues } from '@/schemas/memberSchema'
import type { MailDelivery, Member } from '@/types/member'

/**
 * Member creation. Submitting only opens the confirmation prompt — nothing is sent until the
 * admin confirms, because creating a member also mails that person a code.
 */
export function CreateMemberForm({
  onCreated,
  prefill,
}: {
  onCreated: (member: Member, inviteDelivery: MailDelivery) => void
  /** Name, e-mail and phone carried over from an order, so they are not retyped. */
  prefill?: { email: string; fullName: string; phone: string }
}) {
  const createMember = useCreateMember()
  const [confirming, setConfirming] = useState<CreateMemberFormValues | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateMemberFormValues>({
    resolver: zodResolver(createMemberSchema),
    // `values` rather than `defaultValues`, so the form fills in once the order loads.
    values: {
      email: prefill?.email ?? '',
      fullName: prefill?.fullName ?? '',
      phone: prefill?.phone ?? '',
      sportsBackground: '',
    } as CreateMemberFormValues,
  })

  const onSubmit = (values: CreateMemberFormValues) => {
    createMember.reset()
    setConfirming(values)
  }

  const confirmCreate = () => {
    if (!confirming) {
      return
    }
    createMember.mutate(confirming, {
      onSuccess: ({ member, inviteDelivery }) => {
        setConfirming(null)
        reset()
        onCreated(member, inviteDelivery)
      },
    })
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-5 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Yeni Üye Ekle</h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Üye oluşturulduğunda e-posta adresine 6 haneli bir onay kodu gönderilir. Üyelik, bu kod
          doğrulandığında tamamlanır.
        </p>
      </div>

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="E-Posta *" error={errors.email?.message}>
            <input
              {...register('email')}
              type="email"
              autoComplete="off"
              placeholder="sporcu@example.com"
              className={inputClasses}
            />
          </Field>
          <Field label="Ad Soyad *" error={errors.fullName?.message}>
            <input
              {...register('fullName')}
              type="text"
              autoComplete="off"
              placeholder="Ad Soyad"
              className={inputClasses}
            />
          </Field>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Field label="Telefon *" error={errors.phone?.message}>
            <input
              {...register('phone')}
              type="tel"
              autoComplete="off"
              placeholder="+90 5XX XXX XX XX"
              className={inputClasses}
            />
          </Field>
          <Field label="Boy (cm) *" error={errors.heightCm?.message}>
            <input
              {...register('heightCm', { valueAsNumber: true })}
              type="number"
              min={100}
              max={250}
              step={1}
              placeholder="180"
              className={inputClasses}
            />
          </Field>
          <Field label="Kilo (kg) *" error={errors.weightKg?.message}>
            <input
              {...register('weightKg', { valueAsNumber: true })}
              type="number"
              min={30}
              max={300}
              step="0.1"
              placeholder="82.5"
              className={inputClasses}
            />
          </Field>
        </div>

        <Field label="Spor Geçmişi *" error={errors.sportsBackground?.message}>
          <textarea
            {...register('sportsBackground')}
            rows={4}
            placeholder="Kaç yıldır antrenman yapıyor, hangi branşlar, sakatlık geçmişi…"
            className={textareaClasses}
          />
        </Field>

        {createMember.isError && !confirming ? (
          <p className="text-xs text-error">Üye oluşturulamadı: {createMember.error.message}</p>
        ) : null}

        <Button type="submit" className="w-full sm:w-auto">
          Üyeyi Oluştur
        </Button>
      </form>

      {confirming ? (
        <ConfirmDialog
          title="Üye Oluşturma Onayı"
          confirmLabel="Evet, Oluştur"
          isPending={createMember.isPending}
          error={createMember.isError ? createMember.error.message : null}
          onCancel={() => setConfirming(null)}
          onConfirm={confirmCreate}
          description={
            <>
              <p className="mb-3">Bu kullanıcıyı oluşturmak istediğinize emin misiniz?</p>
              <dl className="space-y-1.5 rounded-xl border border-border bg-surface-elevated p-3.5 text-xs">
                <SummaryRow label="E-Posta" value={confirming.email} />
                <SummaryRow label="Ad Soyad" value={confirming.fullName} />
                <SummaryRow label="Telefon" value={confirming.phone} />
                <SummaryRow label="Boy" value={`${confirming.heightCm} cm`} />
                <SummaryRow label="Kilo" value={`${confirming.weightKg} kg`} />
              </dl>
              <p className="mt-3 text-xs text-text-muted">
                Onayladığınızda bu adrese onay kodu içeren bir e-posta gönderilecek.
              </p>
            </>
          }
        />
      ) : null}
    </section>
  )
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className="break-all text-right text-text-primary">{value}</dd>
    </div>
  )
}
