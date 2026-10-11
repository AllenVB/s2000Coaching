import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'

export function MemberNutritionPage() {
  return (
    <>
      <PageHeading title="Beslenme" description="Makro hedefleriniz ve öğün planınız." />
      <ComingSoonCard
        title="Beslenme Planım"
        description="Makro hedefleriniz ve öğün planınız bu alanda listelenecek."
      />
    </>
  )
}

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
