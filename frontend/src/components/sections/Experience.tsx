import { useEffect, useState } from 'react'

import { useExperiences, useUpdateExperience } from '@/api/experienceApi'
import { Button } from '@/components/ui/Button'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { clearStoredEditToken, getStoredEditToken, setStoredEditToken } from '@/lib/editToken'
import { ApiError } from '@/lib/api'
import type { ExperienceCategory } from '@/types/experience'

const categories: { value: ExperienceCategory; label: string }[] = [
  { value: 'ANTRENMAN', label: 'Antrenman' },
  { value: 'BESLENME', label: 'Beslenme' },
  { value: 'KARDIYO', label: 'Kardiyo' },
]

function promptForEditToken(): string | null {
  const token = window.prompt('Düzenleme parolasını girin:')
  if (!token) {
    return null
  }
  setStoredEditToken(token)
  return token
}

export function Experience() {
  const [activeCategory, setActiveCategory] = useState<ExperienceCategory>('ANTRENMAN')
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const { data: notes, isLoading } = useExperiences()
  const updateExperience = useUpdateExperience()

  const activeNote = notes?.find((note) => note.category === activeCategory)

  useEffect(() => {
    setIsEditing(false)
    setErrorMessage(null)
  }, [activeCategory])

  const startEditing = () => {
    setErrorMessage(null)
    if (!getStoredEditToken() && !promptForEditToken()) {
      return
    }
    setDraft(activeNote?.content ?? '')
    setIsEditing(true)
  }

  const cancelEditing = () => {
    setIsEditing(false)
    setErrorMessage(null)
  }

  const save = () => {
    setErrorMessage(null)
    updateExperience.mutate(
      { category: activeCategory, content: draft },
      {
        onSuccess: () => setIsEditing(false),
        onError: (error) => {
          if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
            clearStoredEditToken()
            setErrorMessage('Parola yanlış görünüyor. Tekrar "Kaydet"e basıp parolayı yeniden girin.')
          } else {
            setErrorMessage('Kaydedilemedi, lütfen tekrar deneyin.')
          }
        },
      },
    )
  }

  const handleSaveClick = () => {
    if (!getStoredEditToken() && !promptForEditToken()) {
      return
    }
    save()
  }

  return (
    <section id="tecrubelerim" className="scroll-mt-24 border-b border-border bg-surface/40 py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="mb-10">
          <SectionHeading
            eyebrow="Sahadan Notlar"
            title="Tecrübelerim"
            description="Antrenman, beslenme ve kardiyo alanlarında yıllar içinde edindiğim tecrübeler."
          />
        </div>

        <div className="mb-8 flex justify-center">
          <div className="inline-flex items-center gap-1 rounded-xl bg-surface p-1.5">
            {categories.map((category) => (
              <button
                key={category.value}
                type="button"
                onClick={() => setActiveCategory(category.value)}
                className={`rounded-lg px-6 py-2.5 text-xs font-semibold uppercase tracking-wider transition-all ${
                  activeCategory === category.value
                    ? 'bg-primary text-canvas'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {category.label}
              </button>
            ))}
          </div>
        </div>

        <div className="mx-auto max-w-3xl rounded-2xl border border-border bg-surface p-6 sm:p-8">
          {isLoading ? (
            <div className="h-40 animate-pulse rounded-xl bg-surface-elevated" />
          ) : isEditing ? (
            <div className="flex flex-col gap-4">
              <textarea
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                rows={10}
                maxLength={20000}
                placeholder="Tecrübelerinizi buraya yazın…"
                className="w-full resize-y rounded-lg border border-border bg-surface-elevated p-4 text-sm leading-relaxed text-text-primary focus:border-primary focus:outline-none"
              />
              {errorMessage ? <p className="text-xs text-error">{errorMessage}</p> : null}
              <div className="flex items-center gap-3">
                <Button onClick={handleSaveClick} disabled={updateExperience.isPending}>
                  {updateExperience.isPending ? 'Kaydediliyor…' : 'Kaydet'}
                </Button>
                <Button variant="secondary" onClick={cancelEditing} disabled={updateExperience.isPending}>
                  İptal
                </Button>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-6">
              {activeNote?.content ? (
                <p className="whitespace-pre-wrap text-sm leading-relaxed text-text-secondary">
                  {activeNote.content}
                </p>
              ) : (
                <p className="text-sm italic text-text-muted">Bu kategori için henüz içerik eklenmedi.</p>
              )}
              <Button variant="secondary" className="w-fit" onClick={startEditing}>
                Düzenle
              </Button>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
