import { Link } from 'react-router-dom'

import { useOrders } from '@/api/ordersAdminApi'
import { Button } from '@/components/ui/Button'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDateTime } from '@/lib/date'
import { formatMinor } from '@/lib/money'

/**
 * Checkout does not create a member, so this page is where a buyer becomes one: the convert
 * action carries the order's details into the member form instead of retyping them.
 */
export function AdminOrdersPage() {
  const { data: orders, isLoading, isError, error } = useOrders()
  useSignOutOn401(error)

  return (
    <>
      <PageHeading
        title="Siparişler"
        description="Üyeliğe dönüşmemiş siparişler işaretlidir; tek tıkla üye oluşturabilirsiniz."
      />

      {isLoading ? (
        <div className="h-24 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Siparişler yüklenemedi: {error.message}
        </p>
      ) : orders && orders.length > 0 ? (
        <ul className="space-y-3">
          {orders.map((order) => (
            <li
              key={order.orderNumber}
              className="flex flex-wrap items-start justify-between gap-4 rounded-2xl border border-border bg-surface p-5"
            >
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2.5">
                  <span className="font-semibold text-text-primary">{order.customerName}</span>
                  {order.memberId ? (
                    <span className="inline-flex items-center rounded-full border border-success/40 bg-success/10 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-success">
                      Üye
                    </span>
                  ) : (
                    <span className="inline-flex items-center rounded-full border border-warning/40 bg-warning/10 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-warning">
                      Üye değil
                    </span>
                  )}
                </div>
                <p className="mt-0.5 break-all text-xs text-text-secondary">
                  {order.email} · {order.phone}
                  {order.cityAge ? ` · ${order.cityAge}` : ''}
                </p>
                <p className="mt-0.5 text-xs text-text-muted">
                  {order.packageName} · {formatDateTime(order.createdAt)} ·{' '}
                  <span className="font-mono">{order.orderNumber}</span>
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                <span className="text-sm font-semibold text-primary">{formatMinor(order.priceMinor)}</span>
                {order.memberId ? (
                  <Link to={`/yonetim/uyeler/${order.memberId}`}>
                    <Button type="button" size="sm" variant="secondary">
                      Üyeyi Aç
                    </Button>
                  </Link>
                ) : (
                  <Link to={`/yonetim/uyeler/yeni?siparis=${order.orderNumber}`}>
                    <Button type="button" size="sm">
                      Üyeye Dönüştür
                    </Button>
                  </Link>
                )}
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Henüz sipariş yok.
        </p>
      )}
    </>
  )
}
