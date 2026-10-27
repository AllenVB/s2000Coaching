import { usePublishedStories } from '@/api/storyApi'
import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { PublicStoryPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { formatDate } from '@/lib/date'
import type { PublicStory } from '@/types/story'

/**
 * Real students, real photos, shown only with their own consent. Replaces the earlier
 * placeholder transformations and testimonials: those used made-up names and stock photos
 * under a "verified results" heading, which a visitor would take as real people.
 *
 * <p>With nothing published the section says so plainly instead of filling the space.
 */
export function SuccessStories() {
  const { data: stories } = usePublishedStories()

  return (
    <section id="basari-hikayeleri" className="scroll-mt-24 border-b border-border py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="mb-14 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <SectionHeading align="left" eyebrow="Gerçek Sonuçlar" title="Değişimi Onlar Yaşadı." />
          <p className="max-w-md text-sm text-text-secondary">
            Burada yalnızca öğrencilerimizin kendi onayıyla paylaşılan, gerçek fotoğraf ve hikâyeler yer alır.
          </p>
        </div>

        {!stories || stories.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-border-strong p-8 text-center text-sm text-text-secondary">
            İlk öğrencilerimizin gerçek sonuçları, kendi onaylarıyla çok yakında burada yayınlanacak.
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {stories.map((story) => (
              <StoryCard key={story.id} story={story} />
            ))}
          </div>
        )}
      </div>
    </section>
  )
}

function StoryCard({ story }: { story: PublicStory }) {
  return (
    <article className="flex flex-col overflow-hidden rounded-3xl border border-border bg-surface">
      <PublicStoryPhotoSourceProvider storyId={story.id}>
        <div className={`grid gap-px bg-border ${story.photos.length > 1 ? 'grid-cols-2' : 'grid-cols-1'}`}>
          {story.photos.slice(0, 2).map((photo) => (
            <figure key={photo.id} className="relative bg-surface">
              <ProgressPhotoImage photo={{ id: photo.id, takenOn: photo.takenOn }} className="aspect-[3/4] w-full !rounded-none" />
              <figcaption className="absolute bottom-2 left-2 rounded-md bg-canvas/90 px-2 py-0.5 text-[11px] font-semibold text-text-primary">
                {photo.caption}
              </figcaption>
            </figure>
          ))}
        </div>
        {story.photos.length > 2 ? (
          <div className="grid grid-cols-4 gap-px bg-border">
            {story.photos.slice(2).map((photo) => (
              <figure key={photo.id} className="bg-surface">
                <ProgressPhotoImage photo={{ id: photo.id, takenOn: photo.takenOn }} className="aspect-square w-full !rounded-none" />
              </figure>
            ))}
          </div>
        ) : null}
      </PublicStoryPhotoSourceProvider>

      <div className="flex flex-1 flex-col p-6">
        <div className="mb-2 flex items-baseline justify-between gap-3">
          <h3 className="text-base font-bold text-text-primary">{story.title}</h3>
          <span className="shrink-0 text-xs text-text-muted">{story.displayName}</span>
        </div>
        {story.quote ? (
          <p className="mb-3 text-sm font-medium italic leading-relaxed text-text-primary">&ldquo;{story.quote}&rdquo;</p>
        ) : null}
        <p className="whitespace-pre-line text-xs leading-relaxed text-text-secondary">{story.body}</p>
        <p className="mt-4 text-[11px] text-text-muted">{formatDate(story.publishedAt)}</p>
      </div>
    </article>
  )
}
