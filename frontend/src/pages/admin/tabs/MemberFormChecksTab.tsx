import { useState } from 'react'

import { useMemberFormChecks, useReplyToFormCheck } from '@/api/formCheckApi'
import { FormCheckVideo } from '@/components/training/FormCheckVideo'
import { Button } from '@/components/ui/Button'
import { textareaClasses } from '@/components/ui/FormField'
import { formatDate } from '@/lib/date'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'
import type { FormCheck } from '@/types/formCheck'

/** Clips the member sent for a form review: watch in the page, answer on the same card. */
export function MemberFormChecksTab() {
  const { member } = useMemberDetail()
  const { data: checks, isLoading, isError, error } = useMemberFormChecks(member.id)

  if (isLoading) {
    return <div className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (isError) {
    return <p className="text-xs text-error">Yüklenemedi: {error.message}</p>
  }
  if (!checks || checks.length === 0) {
    return (
      <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
        Üye henüz form kontrolü videosu göndermedi.
      </p>
    )
  }

  return (
    <div className="space-y-4">
      {checks.map((check) => (
        <FormCheckCard key={check.id} memberId={member.id} check={check} />
      ))}
    </div>
  )
}

function FormCheckCard({ memberId, check }: { memberId: string; check: FormCheck }) {
  const reply = useReplyToFormCheck(memberId)
  const [text, setText] = useState(check.coachReply ?? '')

  return (
    <article className="rounded-2xl border border-border bg-surface p-5">
      <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
        <p className="font-semibold text-text-primary">
          {check.exerciseName} <span className="text-xs font-normal text-text-muted">· {formatDate(check.createdAt)}</span>
        </p>
        {check.coachReply ? (
          <span className="text-[11px] font-semibold text-success">Yanıtlandı</span>
        ) : (
          <span className="text-[11px] font-semibold text-warning">Yanıt bekliyor</span>
        )}
      </div>
      <FormCheckVideo check={check} />
      {check.note ? <p className="mt-3 text-sm leading-relaxed text-text-secondary">{check.note}</p> : null}

      <form
        className="mt-4 space-y-2"
        onSubmit={(event) => {
          event.preventDefault()
          if (text.trim() !== '') {
            reply.mutate({ id: check.id, reply: text.trim() })
          }
        }}
      >
        <textarea
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="Geri bildiriminiz…"
          className={textareaClasses}
        />
        {reply.isError ? <p className="text-xs text-error">{reply.error.message}</p> : null}
        <Button type="submit" size="sm" disabled={reply.isPending || text.trim() === ''}>
          {check.coachReply ? 'Yanıtı Güncelle' : 'Yanıtla'}
        </Button>
      </form>
    </article>
  )
}
