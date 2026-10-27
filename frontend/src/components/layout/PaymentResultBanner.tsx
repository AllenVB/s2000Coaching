import { useState } from 'react'

import { CloseIcon } from '@/components/ui/icons'

const MESSAGES: Record<string, { tone: string; text: (order: string | null) => string }> = {
  basarili: {
    tone: 'border-success/40 bg-success/10 text-success',
    text: (order) =>
      `Ödemeniz alındı${order ? ` (sipariş ${order})` : ''}. Teşekkürler! Koçunuz kısa süre içinde sizinle iletişime geçecek.`,
  },
  basarisiz: {
    tone: 'border-error/40 bg-error/10 text-error',
    text: (order) =>
      `Ödeme tamamlanamadı${order ? ` (sipariş ${order})` : ''}. Kartınızdan çekim yapılmadı; dilerseniz tekrar deneyebilirsiniz.`,
  },
  belirsiz: {
    tone: 'border-warning/40 bg-warning/10 text-warning',
    text: (order) =>
      `Ödemenizin sonucunu şu an doğrulayamadık${order ? ` (sipariş ${order})` : ''}. Kartınızdan çekim yapıldıysa koçunuz siparişi kontrol edip size dönecek; endişe etmeyin.`,
  },
}

/**
 * What the buyer sees when the card page sends them back to the site. The address carries the
 * outcome the server reached (it asked the card provider itself), never anything the browser
 * could claim, so this is only a message — it settles nothing.
 */
export function PaymentResultBanner() {
  const [dismissed, setDismissed] = useState(false)
  const params = new URLSearchParams(window.location.search)
  const result = MESSAGES[params.get('odeme') ?? '']

  if (!result || dismissed) {
    return null
  }

  const close = () => {
    setDismissed(true)
    window.history.replaceState(null, '', window.location.pathname + window.location.hash)
  }

  return (
    <div role="status" className={`mx-auto mt-4 flex max-w-[1280px] items-start justify-between gap-4 rounded-xl border p-4 text-sm ${result.tone}`}>
      <p>{result.text(params.get('siparis'))}</p>
      <button type="button" onClick={close} aria-label="Kapat">
        <CloseIcon className="h-4 w-4" />
      </button>
    </div>
  )
}
