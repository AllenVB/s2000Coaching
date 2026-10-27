import { useState } from 'react'

import { useMembers } from '@/api/adminMembersApi'
import { useMemberPhotos } from '@/api/photosApi'
import {
  useAdminStories,
  useCreateStory,
  useDeleteStory,
  useRemoveStoryPhoto,
  useStoryAction,
  useUpdateStory,
} from '@/api/storyApi'
import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { CoachPhotoSourceProvider, StoryCoachPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDate } from '@/lib/date'
import { TONE_CLASSES } from '@/lib/orderStatus'
import { STORY_STATUS, type Story } from '@/types/story'

/**
 * Success stories with real photos. The order is fixed on purpose: write it, send it to the
 * member, and only after the member has agreed to exactly that content can it be published.
 * Changing the words or photos afterwards sends it back to a draft.
 */
export function StoriesPage() {
  const { data: stories, isLoading, isError, error } = useAdminStories()
  useSignOutOn401(error)

  return (
    <>
      <PageHeading
        title="Başarı Hikâyeleri"
        description="Öğrencinizin gerçek fotoğraf ve hikâyesi; yalnızca öğrencinin açık onayıyla yayınlanır. Onay, öğrencinin kendi hesabından verilir ve istediği an geri çekilebilir."
      />

      <div className="space-y-6">
        <NewStoryForm />

        {isLoading ? (
          <div className="h-24 animate-pulse rounded-2xl border border-border bg-surface" />
        ) : isError ? (
          <p className="text-xs text-error">Yüklenemedi: {error.message}</p>
        ) : !stories || stories.length === 0 ? (
          <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
            Henüz hikâye yok.
          </p>
        ) : (
          stories.map((story) => <StoryCard key={story.id} story={story} />)
        )}
      </div>
    </>
  )
}

function NewStoryForm() {
  const { data: members } = useMembers()
  const createStory = useCreateStory()
  const [memberId, setMemberId] = useState('')
  const [title, setTitle] = useState('')
  const [quote, setQuote] = useState('')
  const [body, setBody] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [picked, setPicked] = useState<Record<string, string>>({})

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const photos = Object.entries(picked).map(([progressPhotoId, caption]) => ({
      progressPhotoId,
      caption: caption.trim() || 'Fotoğraf',
    }))
    createStory.mutate(
      { memberId, title: title.trim(), quote: quote.trim() || null, body: body.trim(), displayName: displayName.trim(), photos },
      {
        onSuccess: () => {
          setTitle('')
          setQuote('')
          setBody('')
          setDisplayName('')
          setPicked({})
        },
      },
    )
  }

  const ready = memberId !== '' && title.trim() !== '' && body.trim() !== '' && displayName.trim() !== '' &&
    Object.keys(picked).length > 0

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Yeni Hikâye</h2>
      <form className="space-y-4" onSubmit={submit}>
        <Field label="Öğrenci *">
          <select
            value={memberId}
            onChange={(event) => {
              setMemberId(event.target.value)
              setPicked({})
              const chosen = members?.find((member) => member.id === event.target.value)
              if (chosen) {
                const [first, ...rest] = chosen.fullName.trim().split(/\s+/)
                setDisplayName(rest.length > 0 ? `${first} ${rest[rest.length - 1][0]}.` : first)
              }
            }}
            className={inputClasses}
          >
            <option value="">Seçin…</option>
            {members?.filter((member) => member.status === 'ACTIVE').map((member) => (
              <option key={member.id} value={member.id}>{member.fullName}</option>
            ))}
          </select>
        </Field>

        {memberId ? (
          <CoachPhotoSourceProvider>
            <MemberPhotoPicker memberId={memberId} picked={picked} onChange={setPicked} />
          </CoachPhotoSourceProvider>
        ) : null}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Başlık *">
            <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={160}
              placeholder="Örn: 16 haftada -12 kg" className={inputClasses} />
          </Field>
          <Field label="Sitede görünecek ad *" hint="Tam ad yerine kısaltma önerilir; öğrenci onay ekranında görür.">
            <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={80}
              className={inputClasses} />
          </Field>
        </div>
        <Field label="Öğrencinin kendi cümlesi" hint="İsteğe bağlı, kısa. Öğrencinin gerçekten söylediği bir söz olmalı.">
          <input value={quote} onChange={(e) => setQuote(e.target.value)} maxLength={400} className={inputClasses} />
        </Field>
        <Field label="Hikâye *" hint="Yalnızca gerçek ve öğrencinin onaylayacağı bilgileri yazın.">
          <textarea value={body} onChange={(e) => setBody(e.target.value)} maxLength={5000} className={textareaClasses} />
        </Field>

        {createStory.isError ? <p className="text-xs text-error">{createStory.error.message}</p> : null}
        <Button type="submit" disabled={!ready || createStory.isPending}>
          {createStory.isPending ? 'Oluşturuluyor…' : 'Taslak Oluştur'}
        </Button>
      </form>
    </section>
  )
}

/** The member's own progress photos; each one picked is copied (and cleaned) into the story. */
function MemberPhotoPicker({
  memberId,
  picked,
  onChange,
}: {
  memberId: string
  picked: Record<string, string>
  onChange: (next: Record<string, string>) => void
}) {
  const { data: photos, isLoading } = useMemberPhotos(memberId)

  if (isLoading) {
    return <div className="h-20 animate-pulse rounded-xl bg-surface-elevated" />
  }
  if (!photos || photos.length === 0) {
    return <p className="text-xs italic text-text-muted">Bu öğrencinin yüklenmiş fotoğrafı yok.</p>
  }

  const toggle = (id: string, defaultCaption: string) => {
    const next = { ...picked }
    if (id in next) {
      delete next[id]
    } else if (Object.keys(next).length < 6) {
      next[id] = defaultCaption
    }
    onChange(next)
  }

  return (
    <div>
      <p className="mb-2 text-xs font-medium text-text-secondary">
        Hikâyeye eklenecek fotoğraflar (en fazla 6; yalnızca JPEG/PNG) — seçilenler temizlenmiş kopya olarak alınır
      </p>
      <ul className="grid grid-cols-3 gap-3 sm:grid-cols-5">
        {photos.map((photo) => (
          <li key={photo.id}>
            <button
              type="button"
              onClick={() => toggle(photo.id, photo.takenOn === photos[photos.length - 1].takenOn ? 'Sonra' : 'Önce')}
              aria-pressed={photo.id in picked}
              className={`block w-full overflow-hidden rounded-xl border-2 ${photo.id in picked ? 'border-primary' : 'border-transparent'}`}
            >
              <ProgressPhotoImage photo={photo} className="aspect-[3/4] w-full" />
            </button>
            <p className="mt-1 text-[11px] text-text-muted">{formatDate(photo.takenOn)}</p>
            {photo.id in picked ? (
              <input
                value={picked[photo.id]}
                onChange={(event) => onChange({ ...picked, [photo.id]: event.target.value })}
                maxLength={60}
                aria-label="Fotoğraf etiketi"
                className={`${inputClasses} mt-1 h-8 text-[11px]`}
              />
            ) : null}
          </li>
        ))}
      </ul>
    </div>
  )
}

function StoryCard({ story }: { story: Story }) {
  const action = useStoryAction()
  const update = useUpdateStory()
  const removePhoto = useRemoveStoryPhoto()
  const deleteStory = useDeleteStory()
  const [editing, setEditing] = useState(false)
  const [title, setTitle] = useState(story.title)
  const [quote, setQuote] = useState(story.quote ?? '')
  const [body, setBody] = useState(story.body)
  const [displayName, setDisplayName] = useState(story.displayName)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  const status = STORY_STATUS[story.status]
  const locked = story.status === 'DECLINED' || story.status === 'REVOKED'

  return (
    <article className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold text-text-primary">{story.title}</h3>
          <p className="text-xs text-text-muted">
            {story.memberName} · {story.displayName} olarak görünür · {formatDate(story.updatedAt)}
          </p>
        </div>
        <span className={`rounded-full border px-2.5 py-1 text-[11px] font-semibold ${TONE_CLASSES[status.tone]}`}>
          {status.label}
        </span>
      </div>

      <StoryCoachPhotoSourceProvider>
        <ul className="mb-3 flex flex-wrap gap-3">
          {story.photos.map((photo) => (
            <li key={photo.id} className="w-24">
              <ProgressPhotoImage photo={photo} className="aspect-[3/4] w-full" />
              <p className="mt-1 truncate text-[11px] text-text-secondary">{photo.caption}</p>
              {!locked ? (
                <button
                  type="button"
                  onClick={() => removePhoto.mutate(photo.id)}
                  className="text-[11px] text-text-muted underline hover:text-error"
                >
                  Çıkar
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      </StoryCoachPhotoSourceProvider>

      {editing ? (
        <form
          className="mb-3 space-y-3"
          onSubmit={(event) => {
            event.preventDefault()
            update.mutate(
              { id: story.id, input: { title: title.trim(), quote: quote.trim() || null, body: body.trim(), displayName: displayName.trim() } },
              { onSuccess: () => setEditing(false) },
            )
          }}
        >
          <input value={title} onChange={(e) => setTitle(e.target.value)} className={inputClasses} aria-label="Başlık" />
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} className={inputClasses} aria-label="Görünen ad" />
          <input value={quote} onChange={(e) => setQuote(e.target.value)} className={inputClasses} aria-label="Öğrencinin cümlesi" />
          <textarea value={body} onChange={(e) => setBody(e.target.value)} className={textareaClasses} aria-label="Hikâye" />
          {story.status !== 'DRAFT' ? (
            <p className="text-[11px] text-warning">
              Değişiklik kaydedilirse öğrencinin onayı geçersiz olur ve hikâye taslağa döner; yeniden onaya
              göndermeniz gerekir.
            </p>
          ) : null}
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={update.isPending}>Kaydet</Button>
            <Button type="button" size="sm" variant="ghost" onClick={() => setEditing(false)}>Vazgeç</Button>
          </div>
          {update.isError ? <p className="text-xs text-error">{update.error.message}</p> : null}
        </form>
      ) : (
        <>
          {story.quote ? <p className="mb-2 text-sm italic text-text-primary">&ldquo;{story.quote}&rdquo;</p> : null}
          <p className="mb-4 whitespace-pre-line text-xs leading-relaxed text-text-secondary">{story.body}</p>
        </>
      )}

      {story.consentAt ? (
        <p className="mb-3 text-[11px] text-success">Öğrenci {formatDate(story.consentAt)} tarihinde onayladı.</p>
      ) : null}

      <div className="flex flex-wrap gap-2">
        {story.status === 'DRAFT' ? (
          <Button size="sm" disabled={action.isPending}
            onClick={() => action.mutate({ id: story.id, action: 'send-for-consent' })}>
            Öğrenciye Onaya Gönder
          </Button>
        ) : null}
        {story.status === 'CONSENTED' ? (
          <Button size="sm" disabled={action.isPending} onClick={() => action.mutate({ id: story.id, action: 'publish' })}>
            Yayınla
          </Button>
        ) : null}
        {story.status === 'PUBLISHED' ? (
          <Button size="sm" variant="secondary" disabled={action.isPending}
            onClick={() => action.mutate({ id: story.id, action: 'unpublish' })}>
            Yayından Kaldır
          </Button>
        ) : null}
        {!locked && !editing ? (
          <Button size="sm" variant="ghost" onClick={() => setEditing(true)}>Düzenle</Button>
        ) : null}
        <Button size="sm" variant="ghost" onClick={() => setConfirmingDelete(true)}>Sil</Button>
      </div>
      {action.isError ? <p className="mt-2 text-xs text-error">{action.error.message}</p> : null}

      {confirmingDelete ? (
        <ConfirmDialog
          title="Hikâye silinsin mi?"
          description="Hikâye ve fotoğraf kopyaları kalıcı olarak silinir. Öğrencinin kendi fotoğraflarına dokunulmaz."
          confirmLabel="Sil"
          tone="danger"
          isPending={deleteStory.isPending}
          error={deleteStory.isError ? deleteStory.error.message : null}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() => deleteStory.mutate(story.id, { onSuccess: () => setConfirmingDelete(false) })}
        />
      ) : null}
    </article>
  )
}
