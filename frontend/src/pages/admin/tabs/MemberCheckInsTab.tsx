import { useState } from 'react'

import { useMemberCheckIns, useReplyToCheckIn } from '@/api/checkInApi'
import { CheckInSummary } from '@/components/member/CheckInSummary'
import { Button } from '@/components/ui/Button'
import { textareaClasses } from '@/components/ui/FormField'
import { formatDate } from '@/lib/date'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'
import type { CheckIn } from '@/types/checkIn'

/** The member's weekly check-ins, newest first, each with a box to answer. */
export function MemberCheckInsTab() {
  const { member } = useMemberDetail()
  const { data: checkIns, isLoading, isError, error } = useMemberCheckIns(member.id)

  if (isLoading) {
    return <div className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (isError) {
    return <p className="text-xs text-error">Yüklenemedi: {error.message}</p>
  }
  if (!checkIns || checkIns.length === 0) {
    return (
      <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
        Üye henüz haftalık değerlendirme göndermedi.
      </p>
    )
  }

  return (
    <div className="space-y-4">
      {checkIns.map((checkIn) => (
        <CheckInCard key={checkIn.id} memberId={member.id} checkIn={checkIn} />
      ))}
    </div>
  )
}

function CheckInCard({ memberId, checkIn }: { memberId: string; checkIn: CheckIn }) {
  const reply = useReplyToCheckIn(memberId)
  const [text, setText] = useState(checkIn.coachReply ?? '')

  return (
    <article className="rounded-2xl border border-border bg-surface p-5">
      <div className="mb-2 flex flex-wrap items-baseline justify-between gap-2">
        <p className="text-sm font-semibold text-text-primary">{formatDate(checkIn.weekStart)} haftası</p>
        {checkIn.coachReply ? (
          <span className="text-[11px] font-semibold text-success">Yanıtlandı</span>
        ) : (
          <span className="text-[11px] font-semibold text-warning">Yanıt bekliyor</span>
        )}
      </div>
      <CheckInSummary checkIn={checkIn} />
      {checkIn.note ? <p className="mt-3 text-sm leading-relaxed text-text-secondary">{checkIn.note}</p> : null}

      <form
        className="mt-4 space-y-2"
        onSubmit={(event) => {
          event.preventDefault()
          if (text.trim() !== '') {
            reply.mutate({ checkInId: checkIn.id, reply: text.trim() })
          }
        }}
      >
        <textarea
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="Yanıtınız…"
          className={textareaClasses}
        />
        {reply.isError ? <p className="text-xs text-error">{reply.error.message}</p> : null}
        <Button type="submit" size="sm" disabled={reply.isPending || text.trim() === ''}>
          {checkIn.coachReply ? 'Yanıtı Güncelle' : 'Yanıtla'}
        </Button>
      </form>
    </article>
  )
}
