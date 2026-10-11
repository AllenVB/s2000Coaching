import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'

export function MemberMessagesPage() {
  return (
    <>
      <PageHeading title="Mesajlar" description="Koçunuzla yazışmanız." />
      <ComingSoonCard
        title="Mesajlar"
        description="Koçunuzla haftalık geri bildirim yazışmanız bu alanda olacak."
      />
    </>
  )
}
