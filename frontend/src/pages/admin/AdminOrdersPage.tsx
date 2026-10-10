import { useAdminDashboard } from '@/api/adminMembersApi'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDateTime } from '@/lib/date'
import { formatMinor } from '@/lib/money'

/**
 * Reads the dashboard's recent-order slice for now. A dedicated, paged orders endpoint
 * lands with the order↔membership link.
 */
export function AdminOrdersPage() {
  const { data, isLoading, isError, error } = useAdminDashboard()
  useSignOutOn401(error)

  return (
    <>
      <PageHeading title="Siparişler" description="Son gelen siparişler." />

      {isLoading ? (
        <div className="h-24 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Siparişler yüklenemedi: {error.message}
        </p>
      ) : data && data.recentOrders.length > 0 ? (
        <ul className="space-y-3">
          {data.recentOrders.map((order) => (
            <li
              key={order.orderNumber}
              className="flex flex-wrap items-start justify-between gap-4 rounded-2xl border border-border bg-surface p-5"
            >
              <div className="min-w-0">
                <p className="font-semibold text-text-primary">{order.customerName}</p>
                <p className="mt-0.5 break-all text-xs text-text-secondary">
                  {order.email} · {order.phone}
                </p>
                <p className="mt-0.5 text-xs text-text-muted">
                  {order.packageName} · {formatDateTime(order.createdAt)} ·{' '}
                  <span className="font-mono">{order.orderNumber}</span>
                </p>
              </div>
              <span className="text-sm font-semibold text-primary">{formatMinor(order.priceMinor)}</span>
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
