import { useState } from 'react'

import { useCompleteRegistration, useDeleteMember, useResendVerificationCode } from '@/api/adminMembersApi'
import { MemberStatusBadge } from '@/components/member/MemberStatusBadge'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import type { Member } from '@/types/member'

export function MemberRow({ member }: { member: Member }) {
  const [isCodeFormOpen, setCodeFormOpen] = useState(false)
  const [code, setCode] = useState('')
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)

  const completeRegistration = useCompleteRegistration()
  const resendCode = useResendVerificationCode()
  const deleteMember = useDeleteMember()

  const isPending = member.status === 'PENDING_VERIFICATION'

  const submitCode = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = code.trim()
    if (trimmed.length === 0) {
      return
    }
    completeRegistration.mutate(
      { memberId: member.id, code: trimmed },
      {
        onSuccess: () => {
          setCode('')
          setCodeFormOpen(false)
        },
      },
    )
  }

  return (
    <li className="rounded-2xl border border-border bg-surface-elevated p-4 sm:p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2.5">
            <span className="font-semibold text-text-primary">{member.fullName}</span>
            <MemberStatusBadge status={member.status} />
          </div>
          <p className="mt-1 break-all text-xs text-text-secondary">{member.email}</p>
          <p className="mt-0.5 text-xs text-text-muted">
            {member.phone} · {member.heightCm} cm · {member.weightKg} kg · Kayıt: {formatDate(member.createdAt)}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {isPending ? (
            <>
              <Button
                type="button"
                size="sm"
                variant="secondary"
                onClick={() => setCodeFormOpen((open) => !open)}
              >
                Onay Kodunu Gir
              </Button>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                onClick={() => resendCode.mutate({ memberId: member.id })}
                disabled={resendCode.isPending}
              >
                {resendCode.isPending ? 'Gönderiliyor…' : 'Kodu Tekrar Gönder'}
              </Button>
            </>
          ) : null}
          <Button
            type="button"
            size="sm"
            variant="danger"
            onClick={() => setConfirmingDelete(true)}
            aria-label={`${member.fullName} üyeliğini sil`}
          >
            <TrashIcon className="h-3.5 w-3.5" />
            Sil
          </Button>
        </div>
      </div>

      <p className="mt-3 whitespace-pre-line text-xs leading-relaxed text-text-secondary">
        <span className="font-semibold uppercase tracking-wider text-text-muted">Spor geçmişi: </span>
        {member.sportsBackground}
      </p>

      {resendCode.isSuccess && !resendCode.isPending ? (
        <p className="mt-2 text-[11px] text-success">Yeni onay kodu {member.email} adresine gönderildi.</p>
      ) : null}
      {resendCode.isError ? <p className="mt-2 text-[11px] text-error">{resendCode.error.message}</p> : null}

      {isPending && isCodeFormOpen ? (
        <form className="mt-3 flex flex-wrap items-start gap-2" onSubmit={submitCode}>
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
          <Button type="submit" size="md" disabled={completeRegistration.isPending}>
            {completeRegistration.isPending ? 'Doğrulanıyor…' : 'Üyeliği Tamamla'}
          </Button>
        </form>
      ) : null}

      {isConfirmingDelete ? (
        <ConfirmDialog
          title="Üye Silme Onayı"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteMember.isPending}
          error={deleteMember.isError ? deleteMember.error.message : null}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() =>
            deleteMember.mutate(
              { memberId: member.id },
              { onSuccess: () => setConfirmingDelete(false) },
            )
          }
          description={
            <>
              <p className="mb-3">Bu kullanıcıyı silmek istediğinize emin misiniz?</p>
              <p className="rounded-xl border border-border bg-surface-elevated p-3.5 text-xs">
                <strong className="text-text-primary">{member.fullName}</strong>
                <br />
                <span className="break-all text-text-secondary">{member.email}</span>
              </p>
              <p className="mt-3 text-xs text-text-muted">
                Üyenin oturumları ve bekleyen onay kodları da silinir. Bu işlem geri alınamaz.
              </p>
            </>
          }
        />
      ) : null}
    </li>
  )
}
