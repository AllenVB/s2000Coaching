import { Link } from 'react-router-dom'

import { useAttention } from '@/api/adminMembersApi'
import { formatMinor } from '@/lib/money'

/** How many rows show before "show all"; the full list is still one click away. */
const SHOWN = 12

const SEVERITY_CLASSES: Record<number, string> = {
  5: 'border-error/40 bg-error/10 text-error',
  4: 'border-error/40 bg-error/10 text-error',
  3: 'border-warning/40 bg-warning/10 text-warning',
  2: 'border-border bg-surface text-text-secondary',
  1: 'border-border bg-surface text-text-secondary',
}

/**
 * The first thing the coach sees: who has gone quiet, whose package is ending, whose message
 * is waiting — most urgent first, each with the reasons spelled out. Computed on the server
 * for the whole roster, so it stays one request however many members there are.
 */
export function AttentionPanel() {
  const { data, isLoading, isError, error } = useAttention()

  if (isLoading) {
    return <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
  }

  if (isError || !data) {
    return (
      <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        İlgi bekleyenler yüklenemedi{error ? `: ${error.message}` : '.'}
      </p>
    )
  }

  const items = data.items

  return (
    <div className="space-y-6">
      {data.pendingTransfers.length > 0 ? (
        <section className="rounded-2xl border border-warning/40 bg-warning/5 p-6">
          <h2 className="mb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
            Havale Bekleyen Siparişler ({data.pendingTransfers.length})
          </h2>
          <ul className="space-y-2">
            {data.pendingTransfers.map((order) => (
              <li
                key={order.orderNumber}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface px-4 py-3 text-sm"
              >
                <span className="min-w-0">
                  <span className="block font-semibold text-text-primary">{order.customerName}</span>
                  <span className="block text-xs text-text-muted">
                    {order.packageName} · <span className="font-mono">{order.orderNumber}</span>
                  </span>
                </span>
                <span className="flex items-center gap-3">
                  <span className="font-semibold text-primary">{formatMinor(order.priceMinor)}</span>
                  <Link to="/yonetim/siparisler" className="text-xs font-semibold text-accent-orange hover:underline">
                    Onayla →
                  </Link>
                </span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
          İlgi Bekleyenler ({items.length})
        </h2>
        <p className="mb-4 text-xs text-text-muted">
          En acilden başlayarak. Yeni katılan biri, katıldığı günden itibaren sayılır; plan verilmemiş
          bir konuda kimse "kayıt yok" diye işaretlenmez.
        </p>

        {items.length === 0 ? (
          <p className="text-sm italic text-text-muted">Şu an ilgi bekleyen üye yok.</p>
        ) : (
          <ul className="space-y-2">
            {items.slice(0, SHOWN).map((item) => (
              <li key={item.memberId}>
                <Link
                  to={`/yonetim/uyeler/${item.memberId}`}
                  className="block rounded-xl border border-border bg-surface-elevated px-4 py-3 transition-colors hover:border-white/20"
                >
                  <span className="block font-semibold text-text-primary">{item.fullName}</span>
                  <span className="mt-1.5 flex flex-wrap gap-1.5">
                    {item.reasons.map((reason) => (
                      <span
                        key={reason.code}
                        className={`rounded-full border px-2.5 py-0.5 text-[11px] font-medium ${
                          SEVERITY_CLASSES[reason.severity] ?? SEVERITY_CLASSES[2]
                        }`}
                      >
                        {reason.label}
                      </span>
                    ))}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        )}
        {items.length > SHOWN ? (
          <p className="mt-3 text-xs text-text-muted">
            ve {items.length - SHOWN} üye daha — Üyeler sekmesinden tümüne ulaşabilirsiniz.
          </p>
        ) : null}
      </section>
    </div>
  )
}
