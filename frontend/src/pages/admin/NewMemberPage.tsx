import { Link, useNavigate, useSearchParams } from 'react-router-dom'

import { useOrder } from '@/api/ordersAdminApi'
import { CreateMemberForm } from '@/components/admin/CreateMemberForm'
import { PageHeading } from '@/components/ui/PageHeading'
import { ArrowLeftIcon, ReceiptIcon } from '@/components/ui/icons'

export function NewMemberPage() {
  const navigate = useNavigate()
  // `?siparis=S2K-…` arrives from the orders page: the buyer's details are carried over so
  // the same person is not typed in twice.
  const [searchParams] = useSearchParams()
  const orderNumber = searchParams.get('siparis')
  const { data: order } = useOrder(orderNumber)

  return (
    <>
      <Link
        to="/yonetim/uyeler"
        className="mb-4 inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-text-muted transition-colors hover:text-text-primary"
      >
        <ArrowLeftIcon className="h-3.5 w-3.5" />
        Üyeler
      </Link>

      <PageHeading
        title="Yeni Üye"
        description="Üye oluşturulduğunda e-posta adresine onay kodu gönderilir; üyelik kod doğrulanınca tamamlanır."
      />

      {order ? (
        <div className="mb-5 flex items-start gap-2 rounded-xl border border-border bg-surface-elevated p-3.5 text-xs leading-relaxed text-text-secondary">
          <ReceiptIcon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <span>
            <span className="font-mono text-text-primary">{order.orderNumber}</span> numaralı siparişten
            dolduruldu ({order.packageName}). Üye oluşturulduğunda bu sipariş üyeye bağlanır.
          </span>
        </div>
      ) : null}

      <CreateMemberForm
        prefill={
          order
            ? { email: order.email, fullName: order.customerName, phone: order.phone }
            : undefined
        }
        // The delivery rides along in the navigation state, so the member's page can say the
        // invitation did not go out instead of looking like everything worked.
        onCreated={(member, inviteDelivery) =>
          navigate(`/yonetim/uyeler/${member.id}`, { state: { inviteDelivery } })
        }
      />
    </>
  )
}
