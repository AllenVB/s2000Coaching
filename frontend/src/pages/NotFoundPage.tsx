import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <main className="mx-auto flex min-h-screen w-full max-w-md flex-col items-center justify-center px-4 text-center">
      <span className="font-display text-xs font-bold uppercase tracking-[0.2em] text-primary">404</span>
      <h1 className="mt-3 font-display text-3xl font-extrabold uppercase tracking-tight text-text-primary">
        Sayfa Bulunamadı
      </h1>
      <p className="mt-3 text-sm text-text-secondary">
        Aradığınız sayfa taşınmış veya hiç var olmamış olabilir.
      </p>
      <Link
        to="/"
        className="mt-6 inline-flex h-11 items-center justify-center rounded-xl bg-primary px-5 text-sm font-semibold uppercase tracking-wide text-canvas transition-colors hover:bg-primary-hover"
      >
        Ana Sayfaya Dön
      </Link>
    </main>
  )
}
