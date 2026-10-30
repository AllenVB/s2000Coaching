import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useLegalDocument } from '@/api/siteApi'
import { SimplePublicHeader } from '@/components/layout/SimplePublicHeader'
import { ApiError } from '@/lib/api'
import { formatDate } from '@/lib/date'

/**
 * One of the legal pages (terms, privacy notice, distance-sales contract, refund policy). The text
 * is whatever the coach has published; until then the page says so plainly instead of showing a
 * made-up text, and it is shown as plain paragraphs so it can never carry markup.
 */
export function LegalPage() {
  const { slug = '' } = useParams<{ slug: string }>()
  const { data, isLoading, error } = useLegalDocument(slug)

  useEffect(() => {
    if (!data) {
      return
    }
    const previous = document.title
    document.title = `${data.title} — S2000 Coaching`
    return () => {
      document.title = previous
    }
  }, [data])

  const notFound = error instanceof ApiError && error.status === 404

  return (
    <div className="min-h-screen">
      <SimplePublicHeader />
      <main className="mx-auto w-full max-w-[760px] px-4 py-10 sm:px-6 sm:py-14">
        {isLoading ? <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" /> : null}

        {notFound ? (
          <>
            <h1 className="font-display text-3xl font-extrabold uppercase tracking-tight text-text-primary">Sayfa bulunamadı</h1>
            <p className="mt-3 text-sm text-text-secondary">Aradığın yasal metin bu adreste yok.</p>
          </>
        ) : error ? (
          <p role="alert" className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
            Sayfa yüklenemedi: {error.message}
          </p>
        ) : null}

        {data ? (
          <>
            <h1 className="font-display text-3xl font-extrabold uppercase tracking-tight text-text-primary sm:text-4xl">
              {data.title}
            </h1>
            {data.content ? (
              <>
                {data.updatedAt ? (
                  <p className="mt-2 text-xs text-text-muted">Son güncelleme: {formatDate(data.updatedAt)}</p>
                ) : null}
                <div className="mt-8 whitespace-pre-line text-sm leading-relaxed text-text-secondary">{data.content}</div>
              </>
            ) : (
              <p className="mt-6 rounded-2xl border border-border bg-surface p-6 text-sm leading-relaxed text-text-secondary">
                Bu metin henüz yayınlanmadı. Sorularınız için koçunuzla iletişime geçebilirsiniz.
              </p>
            )}
          </>
        ) : null}

        <Link to="/" className="mt-10 inline-block text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
          ← Ana sayfaya dön
        </Link>
      </main>
    </div>
  )
}
