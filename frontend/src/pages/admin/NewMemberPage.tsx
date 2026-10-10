import { Link, useNavigate } from 'react-router-dom'

import { CreateMemberForm } from '@/components/admin/CreateMemberForm'
import { PageHeading } from '@/components/ui/PageHeading'
import { ArrowLeftIcon } from '@/components/ui/icons'

export function NewMemberPage() {
  const navigate = useNavigate()

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

      <CreateMemberForm onCreated={(member) => navigate(`/yonetim/uyeler/${member.id}`)} />
    </>
  )
}
