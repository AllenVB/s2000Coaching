import { useState } from 'react'

import { useMyStories, useMyStoryDecision } from '@/api/storyApi'
import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { Button } from '@/components/ui/Button'
import { StoryMemberPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { formatDate } from '@/lib/date'
import { STORY_STATUS, type Story } from '@/types/story'

/**
 * Where a member answers "may we show your story on the site?" — the only place that can say
 * yes. Renders nothing until the coach has proposed a story, so it costs the page no space.
 */
export function StoryConsentPanel() {
  const { data: stories } = useMyStories()

  if (!stories || stories.length === 0) {
    return null
  }

  return (
    <section aria-label="Başarı hikâyesi onayı" className="space-y-4">
      <div>
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Başarı Hikâyem</h2>
        <p className="mt-1 text-xs text-text-secondary">
          Koçunuz hikâyenizi sitede paylaşmak istiyor. Onay vermek tamamen sizin kararınızdır.
        </p>
      </div>
      <StoryMemberPhotoSourceProvider>
        {stories.map((story) => (
          <StoryDecisionCard key={story.id} story={story} />
        ))}
      </StoryMemberPhotoSourceProvider>
    </section>
  )
}

function StoryDecisionCard({ story }: { story: Story }) {
  const decide = useMyStoryDecision()
  const [agreed, setAgreed] = useState(false)
  const status = STORY_STATUS[story.status]

  return (
    <article className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <h2 className="font-semibold text-text-primary">{story.title}</h2>
        <span className="text-xs font-semibold text-text-secondary">{status.label}</span>
      </div>

      {story.photos.length > 0 ? (
        <ul className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {story.photos.map((photo) => (
            <li key={photo.id}>
              <ProgressPhotoImage photo={photo} className="aspect-[3/4] w-full" />
              <p className="mt-1 text-[11px] text-text-secondary">{photo.caption}</p>
            </li>
          ))}
        </ul>
      ) : null}

      <p className="mb-1 text-xs text-text-muted">Sitede şu adla görünür: <strong className="text-text-primary">{story.displayName}</strong></p>
      {story.quote ? <p className="my-2 text-sm italic text-text-primary">&ldquo;{story.quote}&rdquo;</p> : null}
      <p className="whitespace-pre-line text-sm leading-relaxed text-text-secondary">{story.body}</p>

      {story.status === 'AWAITING_CONSENT' ? (
        <div className="mt-5 space-y-3 rounded-xl border border-primary/30 bg-primary/5 p-4">
          <p className="text-xs leading-relaxed text-text-primary">{story.consentText}</p>
          <label className="flex cursor-pointer items-start gap-2 text-xs text-text-primary">
            <input type="checkbox" checked={agreed} onChange={(e) => setAgreed(e.target.checked)} className="mt-0.5" />
            Yukarıdaki metni okudum; fotoğraflarım ve hikâyem için açık rıza veriyorum.
          </label>
          <div className="flex flex-wrap gap-2">
            <Button size="sm" disabled={!agreed || decide.isPending}
              onClick={() => decide.mutate({ id: story.id, decision: 'consent' })}>
              Onaylıyorum
            </Button>
            <Button size="sm" variant="ghost" disabled={decide.isPending}
              onClick={() => decide.mutate({ id: story.id, decision: 'decline' })}>
              Kabul etmiyorum
            </Button>
          </div>
          <p className="text-[11px] text-text-muted">
            "Kabul etmiyorum" dediğinizde fotoğraf kopyaları silinir ve hikâye hiçbir yerde yayınlanmaz.
          </p>
        </div>
      ) : null}

      {story.status === 'CONSENTED' || story.status === 'PUBLISHED' ? (
        <div className="mt-5 space-y-2">
          <p className="text-xs text-success">
            {story.consentAt ? `${formatDate(story.consentAt)} tarihinde onay verdiniz.` : 'Onay verdiniz.'}
            {story.status === 'PUBLISHED' ? ' Hikâyeniz şu an sitede yayında.' : ' Henüz yayınlanmadı.'}
          </p>
          <Button size="sm" variant="secondary" disabled={decide.isPending}
            onClick={() => decide.mutate({ id: story.id, decision: 'revoke' })}>
            Onayımı Geri Çekiyorum
          </Button>
          <p className="text-[11px] text-text-muted">
            Geri çektiğinizde hikâye hemen yayından kalkar ve fotoğraf kopyaları silinir.
          </p>
        </div>
      ) : null}

      {story.status === 'DECLINED' ? (
        <p className="mt-4 text-xs text-text-muted">Bu hikâyeyi kabul etmediniz; hiçbir yerde yayınlanmaz.</p>
      ) : null}
      {story.status === 'REVOKED' ? (
        <p className="mt-4 text-xs text-text-muted">Onayınızı geri çektiniz; hikâye yayından kalktı ve fotoğraf kopyaları silindi.</p>
      ) : null}
      {decide.isError ? <p className="mt-3 text-xs text-error">{decide.error.message}</p> : null}
    </article>
  )
}
