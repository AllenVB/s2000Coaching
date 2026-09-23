import { useEffect, useState } from 'react'

import { useExperienceCategories } from '@/api/experienceApi'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { EyeIcon, PencilIcon } from '@/components/ui/icons'

import { ExperienceAddCategoryTab } from './ExperienceAddCategoryTab'
import { ExperienceAddItemCard } from './ExperienceAddItemCard'
import { ExperienceCategoryTab } from './ExperienceCategoryTab'
import { ExperienceItemCard } from './ExperienceItemCard'

type ViewMode = 'read' | 'edit'

export function Experience() {
  const { data: categories, isLoading } = useExperienceCategories()
  const [mode, setMode] = useState<ViewMode>('read')
  const [activeCategoryId, setActiveCategoryId] = useState<string | null>(null)

  useEffect(() => {
    if (!categories || categories.length === 0) {
      setActiveCategoryId(null)
      return
    }
    if (!activeCategoryId || !categories.some((category) => category.id === activeCategoryId)) {
      setActiveCategoryId(categories[0].id)
    }
  }, [categories, activeCategoryId])

  const isEditMode = mode === 'edit'
  const activeCategory = categories?.find((category) => category.id === activeCategoryId) ?? null

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

        <div className="flex flex-col gap-8 lg:flex-row lg:items-start">
          <div className="min-w-0 flex-1">
            {isLoading ? (
              <div className="grid gap-4 sm:grid-cols-2">
                {[0, 1].map((i) => (
                  <div key={i} className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
                ))}
              </div>
            ) : (
              <>
                <div className="mb-8 flex flex-wrap items-center gap-2">
                  {categories?.map((category) => (
                    <ExperienceCategoryTab
                      key={category.id}
                      category={category}
                      isActive={category.id === activeCategoryId}
                      isEditMode={isEditMode}
                      onSelect={() => setActiveCategoryId(category.id)}
                    />
                  ))}
                  {isEditMode ? (
                    <ExperienceAddCategoryTab onCreated={setActiveCategoryId} />
                  ) : null}
                </div>

                {activeCategory ? (
                  <div className="grid gap-4 sm:grid-cols-2">
                    {activeCategory.items.map((item) => (
                      <ExperienceItemCard key={item.id} item={item} isEditMode={isEditMode} />
                    ))}
                    {isEditMode ? (
                      <ExperienceAddItemCard categoryId={activeCategory.id} />
                    ) : activeCategory.items.length === 0 ? (
                      <p className="text-sm italic text-text-muted sm:col-span-2">
                        Bu kategori için henüz tecrübe eklenmedi.
                      </p>
                    ) : null}
                  </div>
                ) : (
                  <p className="text-sm italic text-text-muted">
                    {isEditMode
                      ? 'Başlamak için sağdaki "Düzenleme Modu"ndayken bir kategori ekleyin.'
                      : 'Henüz kategori eklenmedi.'}
                  </p>
                )}
              </>
            )}
          </div>

          <div className="lg:sticky lg:top-24 lg:w-56 lg:shrink-0">
            <div className="rounded-2xl border border-border bg-surface p-4">
              <span className="mb-3 block text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                Görünüm
              </span>
              <div className="flex flex-col gap-2">
                <button
                  type="button"
                  onClick={() => setMode('read')}
                  className={`flex items-center gap-2.5 rounded-xl px-3.5 py-3 text-left text-sm font-semibold transition-all ${
                    mode === 'read'
                      ? 'bg-primary text-canvas shadow-[0_0_20px_rgba(199,240,0,0.2)]'
                      : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
                  }`}
                >
                  <EyeIcon className="h-4 w-4 shrink-0" />
                  Okuma Modu
                </button>
                <button
                  type="button"
                  onClick={() => setMode('edit')}
                  className={`flex items-center gap-2.5 rounded-xl px-3.5 py-3 text-left text-sm font-semibold transition-all ${
                    mode === 'edit'
                      ? 'bg-primary text-canvas shadow-[0_0_20px_rgba(199,240,0,0.2)]'
                      : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
                  }`}
                >
                  <PencilIcon className="h-4 w-4 shrink-0" />
                  Düzenleme Modu
                </button>
              </div>
              <p className="mt-3 text-[11px] leading-relaxed text-text-muted">
                {isEditMode
                  ? 'Kategori ve tecrübe ekleyip düzenleyebilirsiniz.'
                  : 'Ziyaretçilerin gördüğü halidir.'}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
