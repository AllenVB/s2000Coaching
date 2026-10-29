import { Link } from 'react-router-dom'

import type { MailDelivery } from '@/types/member'

/**
 * Says whether a mailed code actually left. A failure is the coach's to fix — the member cannot
 * log in without the code — so it is spelled out with the way forward, not left as a quiet log
 * line. `what` names the message ("Onay kodu", "Davet").
 */
export function MailDeliveryNotice({ delivery, what = 'Onay kodu' }: { delivery: MailDelivery; what?: string }) {
  if (delivery === 'SENT') {
    return <p className="text-[11px] text-success">{what} e-postayla gönderildi.</p>
  }

  if (delivery === 'NOT_CONFIGURED') {
    return (
      <p className="rounded-lg border border-warning/40 bg-warning/10 p-3 text-xs leading-relaxed text-warning">
        Sunucuda e-posta (SMTP) tanımlı değil, bu yüzden {what.toLowerCase()} gönderilmedi; yalnızca sunucu
        günlüğüne yazıldı. Gerçek üyeler için <code>MAIL_HOST</code> ayarlanmalı.
      </p>
    )
  }

  return (
    <p
      role="alert"
      className="rounded-lg border border-error/40 bg-error/10 p-3 text-xs leading-relaxed text-error"
    >
      <strong>{what} e-postayla gönderilemedi.</strong> Üye oluşturuldu ama kodu alamadı, giriş yapamaz. E-posta
      ayarlarını düzeltin (nedeni{' '}
      <Link to="/yonetim/ayarlar" className="font-semibold underline">
        Ayarlar → Test e-postası
      </Link>{' '}
      gösterir), sonra &quot;Kodu Tekrar Gönder&quot;e basın.
    </p>
  )
}
