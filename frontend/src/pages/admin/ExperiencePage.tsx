import { Experience } from '@/components/sections/Experience'
import { PageHeading } from '@/components/ui/PageHeading'

/**
 * "Tecrübelerim", moved off the public home page: the coach's notes by category and topic, shown as
 * they were on the site. It is read-only for now; the content is still the seeded text and the coach
 * will rewrite it.
 */
export function ExperiencePage() {
  return (
    <>
      <PageHeading
        title="Tecrübelerim"
        description="Ana sayfadan kaldırıldı; yalnızca yönetim panelinde görünür. İçeriği daha sonra düzenlenecek."
      />
      <div className="overflow-hidden rounded-2xl border border-border">
        <Experience />
      </div>
    </>
  )
}
