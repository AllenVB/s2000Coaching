import { useEffect, useMemo, useState } from 'react'

import { useExperienceCategories } from '@/api/experienceApi'
import { SectionHeading } from '@/components/ui/SectionHeading'

export function Experience() {
  const { data: categories, isLoading } = useExperienceCategories()
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(null)
  const [activeTopicId, setActiveTopicId] = useState<string | null>(null)

  const activeCategory = useMemo(
    () => categories?.find((category) => category.id === activeCategoryId) ?? null,
    [categories, activeCategoryId],
  )

  // Default to the first category once data arrives (and recover if it disappears).
  useEffect(() => {
    if (!categories || categories.length === 0) {
      setActiveCategoryId(null)
      return
    }
    if (!activeCategoryId || !categories.some((category) => category.id === activeCategoryId)) {
      setActiveCategoryId(categories[0].id)
    }
  }, [categories, activeCategoryId])

  // Default to the active category's first topic whenever the category changes.
  useEffect(() => {
    if (!activeCategory || activeCategory.topics.length === 0) {
      setActiveTopicId(null)
      return
    }
    if (!activeTopicId || !activeCategory.topics.some((topic) => topic.id === activeTopicId)) {
      setActiveTopicId(activeCategory.topics[0].id)
    }
  }, [activeCategory, activeTopicId])

  const activeTopic = activeCategory?.topics.find((topic) => topic.id === activeTopicId) ?? null

  return (
    <section id="tecrubelerim" className="scroll-mt-24 border-b border-border bg-surface/40 py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="mb-10 mx-auto">
          <SectionHeading
            eyebrow="Sahadan Notlar"
            title="Tecrübelerim"
            description="Antrenman, beslenme ve kardiyo alanlarında yıllar içinde edindiğim tecrübeler."
          />
        </div>

        {/* Category tabs */}
        <div className="mb-8 flex justify-center">
          <div className="inline-flex flex-wrap items-center justify-center gap-1 rounded-xl bg-surface p-1.5">
            {categories?.map((category) => (
              <button
                key={category.id}
                type="button"
                onClick={() => setActiveCategoryId(category.id)}
                className={`rounded-lg px-6 py-2.5 text-xs font-semibold uppercase tracking-wider transition-all ${
                  category.id === activeCategoryId
                    ? 'bg-primary text-canvas'
                    : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                {category.name}
              </button>
            ))}
          </div>
        </div>

        {isLoading ? (
          <div className="h-48 animate-pulse rounded-2xl border border-border bg-surface" />
        ) : !activeCategory ? (
          <p className="text-center text-sm italic text-text-muted">Henüz içerik eklenmedi.</p>
        ) : activeCategory.topics.length === 0 ? (
          <p className="text-center text-sm italic text-text-muted">
            Bu kategoride henüz başlık eklenmedi.
          </p>
        ) : (
          <div className="flex flex-col gap-8 lg:flex-row lg:items-start">
            {/* Topic sidebar */}
            <aside className="lg:sticky lg:top-24 lg:w-56 lg:shrink-0">
              <div className="flex gap-2 overflow-x-auto rounded-2xl border border-border bg-surface p-2 lg:flex-col lg:overflow-visible">
                {activeCategory.topics.map((topic) => {
                  const isActive = topic.id === activeTopicId
                  return (
                    <button
                      key={topic.id}
                      type="button"
                      onClick={() => setActiveTopicId(topic.id)}
                      className={`shrink-0 whitespace-nowrap rounded-xl px-4 py-2.5 text-left text-sm font-semibold transition-all lg:w-full ${
                        isActive
                          ? 'bg-accent-orange/10 text-accent-orange lg:border-l-2 lg:border-accent-orange'
                          : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
                      }`}
                    >
                      {topic.name}
                    </button>
                  )
                })}
              </div>
            </aside>

            {/* Notes for the active topic */}
            <div className="min-w-0 flex-1">
              {activeTopic ? (
                <>
                  <h3 className="mb-5 font-display text-xl font-bold uppercase tracking-tight text-accent-orange">
                    {activeTopic.name}
                  </h3>
                  {activeTopic.notes.length > 0 ? (
                    <div className="flex flex-col gap-4">
                      {activeTopic.notes.map((note) => (
                        <div key={note.id} className="rounded-2xl border border-border bg-surface p-5 sm:p-6">
                          <p className="whitespace-pre-wrap text-sm leading-relaxed text-text-primary sm:text-base">
                            {note.content}
                          </p>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm italic text-text-muted">İçerik yakında eklenecek.</p>
                  )}
                </>
              ) : null}
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
