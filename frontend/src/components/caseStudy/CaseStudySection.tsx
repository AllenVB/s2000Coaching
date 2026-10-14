import { useState } from 'react'

import { usePublicCaseStudy, usePublishedCaseStudies } from '@/api/caseStudyApi'
import { AnalysisReport } from '@/components/caseStudy/AnalysisReport'

/**
 * The published analysis of my own body data, at the end of Tecrübelerim.
 *
 * <p>Renders nothing at all when no study is published. An empty "Kendi Verim" heading on
 * the landing page would promise something that is not there, and the section only exists
 * once there is data behind it.
 */
export function CaseStudySection() {
  const { data: studies } = usePublishedCaseStudies()
  const [chosenSlug, setChosenSlug] = useState<string | null>(null)

  // Derived rather than synced through an effect: the newest study is open until one is
  // picked, and a choice that stops being published falls back instead of showing nothing.
  const slug =
    chosenSlug && studies?.some((study) => study.slug === chosenSlug)
      ? chosenSlug
      : studies?.[0]?.slug ?? null

  const { data: study, isLoading, isError } = usePublicCaseStudy(slug)

  if (!studies || studies.length === 0) {
    return null
  }

  return (
    <div className="mt-16 border-t border-border pt-12">
      <div className="mb-8">
        <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.2em] text-accent-orange">
          Kendi Verim
        </p>
        <h3 className="font-display text-2xl font-bold uppercase tracking-tight text-text-primary sm:text-3xl">
          {study?.title ?? studies[0].title}
        </h3>
        {study?.summary ? (
          <p className="mt-3 max-w-3xl text-sm leading-relaxed text-text-secondary">
            {study.summary}
          </p>
        ) : null}
      </div>

      {studies.length > 1 ? (
        <div className="mb-8 flex flex-wrap gap-2">
          {studies.map((option) => (
            <button
              key={option.slug}
              type="button"
              onClick={() => setChosenSlug(option.slug)}
              className={`rounded-xl px-4 py-2 text-xs font-semibold uppercase tracking-wider transition-all ${
                option.slug === slug
                  ? 'bg-primary text-canvas'
                  : 'bg-surface text-text-secondary hover:text-text-primary'
              }`}
            >
              {option.title}
            </button>
          ))}
        </div>
      ) : null}

      {isLoading ? (
        <div className="h-64 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError || !study ? (
        <p className="text-sm italic text-text-muted">Analiz şu an yüklenemedi.</p>
      ) : (
        <AnalysisReport analysis={study.analysis} />
      )}
    </div>
  )
}
