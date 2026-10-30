import { useState, type FormEvent } from 'react'

import { useAdminSiteSettings, useUpdateLegal, useUpdateSocial, useUpdateStats } from '@/api/siteApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { formatDate } from '@/lib/date'
import { trustStats } from '@/data/content'
import type { AdminSiteSettings, LegalDocument, SiteStat } from '@/types/site'

/** What the public site shows, edited without a deployment: social links, home-page figures, legal pages. */
export function SiteSettingsEditor() {
  const { data, isLoading, error } = useAdminSiteSettings()

  if (isLoading) {
    return <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (error || !data) {
    return (
      <p role="alert" className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        Site ayarları yüklenemedi: {error?.message}
      </p>
    )
  }

  return (
    <>
      <SocialCard settings={data} />
      <StatsCard settings={data} />
      <LegalCard documents={data.legal} />
    </>
  )
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">{title}</h2>
      {children}
    </section>
  )
}

function Status({ pending, error, saved }: { pending: boolean; error: Error | null; saved: boolean }) {
  if (error) {
    return <p role="alert" className="text-xs text-error">{error.message}</p>
  }
  return saved && !pending ? <span className="text-xs text-success">Kaydedildi</span> : null
}

/* ---------- social links ---------- */

function SocialCard({ settings }: { settings: AdminSiteSettings }) {
  const update = useUpdateSocial()
  const [instagram, setInstagram] = useState(settings.social.instagram ?? '')
  const [youtube, setYoutube] = useState(settings.social.youtube ?? '')
  const [whatsapp, setWhatsapp] = useState(settings.social.whatsapp ?? '')

  const submit = (event: FormEvent) => {
    event.preventDefault()
    update.mutate({ instagram, youtube, whatsapp })
  }

  return (
    <Card title="Sosyal Bağlantılar">
      <p className="mt-2 text-xs leading-relaxed text-text-secondary">
        Sitenin altında yalnızca doldurduklarınız görünür; boş bıraktığınız bağlantı hiç gösterilmez.
      </p>
      <form className="mt-4 max-w-xl space-y-4" onSubmit={submit}>
        <Field label="Instagram" hint="Kullanıcı adı (@s2000coaching) ya da instagram.com adresi">
          <input value={instagram} onChange={(event) => setInstagram(event.target.value)} type="text" autoComplete="off" className={inputClasses} />
        </Field>
        <Field label="YouTube" hint="youtube.com ya da youtu.be adresi">
          <input value={youtube} onChange={(event) => setYoutube(event.target.value)} type="text" autoComplete="off" className={inputClasses} />
        </Field>
        <Field label="WhatsApp" hint="Telefon numarası (+90 555 111 22 33) ya da wa.me adresi">
          <input value={whatsapp} onChange={(event) => setWhatsapp(event.target.value)} type="text" autoComplete="off" className={inputClasses} />
        </Field>
        <div className="flex items-center gap-3">
          <Button type="submit" size="sm" disabled={update.isPending}>
            {update.isPending ? 'Kaydediliyor…' : 'Kaydet'}
          </Button>
          <Status pending={update.isPending} error={update.error} saved={update.isSuccess} />
        </div>
      </form>
    </Card>
  )
}

/* ---------- home-page figures ---------- */

const EMPTY_STAT: SiteStat = { label: '', value: '', caption: '', footnote: '', accent: false }

function StatsCard({ settings }: { settings: AdminSiteSettings }) {
  const update = useUpdateStats()
  const custom = settings.stats !== null
  const [stats, setStats] = useState<SiteStat[]>(
    settings.stats ?? trustStats.map((stat) => ({ ...stat, accent: stat.accent === true })),
  )

  const change = (index: number, patch: Partial<SiteStat>) =>
    setStats((current) => current.map((stat, at) => (at === index ? { ...stat, ...patch } : stat)))

  const submit = (event: FormEvent) => {
    event.preventDefault()
    update.mutate({ stats })
  }

  const reset = () => {
    update.mutate(
      { stats: null },
      { onSuccess: () => setStats(trustStats.map((stat) => ({ ...stat, accent: stat.accent === true }))) },
    )
  }

  return (
    <Card title="Ana Sayfa Rakamları">
      <p className="mt-2 text-xs leading-relaxed text-text-secondary">
        Ana sayfada kahramanın altındaki dört kart. Burada yazdığınız her rakamın doğruluğundan siz
        sorumlusunuz: sitedeki örnek değerleri gerçek sayılarınızla değiştirin ya da gerekmiyorsa kartları
        silin.{' '}
        {custom ? 'Şu an sizin yazdığınız değerler gösteriliyor.' : 'Şu an sitenin hazır (örnek) değerleri gösteriliyor.'}
      </p>
      <form className="mt-4 space-y-4" onSubmit={submit}>
        {stats.map((stat, index) => (
          <fieldset key={index} className="rounded-xl border border-border bg-surface-elevated p-4">
            <legend className="px-1 text-[11px] font-semibold uppercase tracking-wider text-text-muted">Kart {index + 1}</legend>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <Field label="Başlık">
                <input value={stat.label} onChange={(event) => change(index, { label: event.target.value })} maxLength={40} className={inputClasses} />
              </Field>
              <Field label="Rakam">
                <input value={stat.value} onChange={(event) => change(index, { value: event.target.value })} maxLength={20} className={inputClasses} />
              </Field>
              <Field label="Açıklama">
                <input value={stat.caption} onChange={(event) => change(index, { caption: event.target.value })} maxLength={60} className={inputClasses} />
              </Field>
            </div>
            <div className="mt-3">
              <Field label="Alt not">
                <input value={stat.footnote} onChange={(event) => change(index, { footnote: event.target.value })} maxLength={200} className={inputClasses} />
              </Field>
            </div>
            <div className="mt-3 flex items-center justify-between">
              <label className="flex items-center gap-2 text-xs text-text-secondary">
                <input type="checkbox" checked={stat.accent} onChange={(event) => change(index, { accent: event.target.checked })} />
                Vurgulu (turuncu rakam)
              </label>
              <button
                type="button"
                onClick={() => setStats((current) => current.filter((_stat, at) => at !== index))}
                className="text-xs font-semibold text-text-muted transition-colors hover:text-error"
              >
                Kartı sil
              </button>
            </div>
          </fieldset>
        ))}
        {stats.length === 0 ? (
          <p className="text-xs text-text-muted">Kart yok: kaydederseniz bu bölüm ana sayfada hiç görünmez.</p>
        ) : null}

        <div className="flex flex-wrap items-center gap-3">
          {stats.length < 4 ? (
            <Button type="button" size="sm" variant="secondary" onClick={() => setStats((current) => [...current, { ...EMPTY_STAT }])}>
              Kart Ekle
            </Button>
          ) : null}
          <Button type="submit" size="sm" disabled={update.isPending}>
            {update.isPending ? 'Kaydediliyor…' : 'Kaydet'}
          </Button>
          {custom ? (
            <Button type="button" size="sm" variant="ghost" onClick={reset} disabled={update.isPending}>
              Hazır değerlere dön
            </Button>
          ) : null}
          <Status pending={update.isPending} error={update.error} saved={update.isSuccess} />
        </div>
      </form>
    </Card>
  )
}

/* ---------- legal pages ---------- */

function LegalCard({ documents }: { documents: LegalDocument[] }) {
  return (
    <Card title="Yasal Sayfalar">
      <p className="mt-2 text-xs leading-relaxed text-text-secondary">
        Sitenin altındaki ve ödeme penceresindeki bağlantılar bu metinleri gösterir. Metni hukuk
        danışmanınızın onayladığı haliyle yapıştırın; sistem kendi başına bir metin yazmaz. Yazılmamış bir
        sayfa ziyaretçiye "henüz yayınlanmadı" der. Metni silip kaydetmek sayfayı yayından kaldırır.
      </p>
      <div className="mt-4 space-y-6">
        {documents.map((document) => (
          <LegalEditor key={`${document.slug}-${document.updatedAt ?? 'new'}`} document={document} />
        ))}
      </div>
    </Card>
  )
}

function LegalEditor({ document }: { document: LegalDocument }) {
  const update = useUpdateLegal()
  const [content, setContent] = useState(document.content ?? '')

  const submit = (event: FormEvent) => {
    event.preventDefault()
    update.mutate({ slug: document.slug, content })
  }

  return (
    <form onSubmit={submit} className="space-y-2 border-t border-border pt-4 first:border-t-0 first:pt-0">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-xs font-bold uppercase tracking-wider text-text-primary">{document.title}</h3>
        <span className={`text-[11px] ${document.content ? 'text-success' : 'text-text-muted'}`}>
          {document.content
            ? `Yayında${document.updatedAt ? ` · ${formatDate(document.updatedAt)}` : ''}`
            : 'Yayınlanmadı'}
        </span>
      </div>
      <textarea
        value={content}
        onChange={(event) => setContent(event.target.value)}
        maxLength={100_000}
        rows={6}
        aria-label={`${document.title} metni`}
        placeholder="Metni buraya yapıştırın…"
        className={textareaClasses}
      />
      <div className="flex flex-wrap items-center gap-3">
        <Button type="submit" size="sm" disabled={update.isPending}>
          {update.isPending ? 'Kaydediliyor…' : 'Kaydet'}
        </Button>
        <a href={`/yasal/${document.slug}`} target="_blank" rel="noopener noreferrer" className="text-xs font-semibold text-primary hover:underline">
          Sayfayı gör
        </a>
        <Status pending={update.isPending} error={update.error} saved={update.isSuccess} />
      </div>
    </form>
  )
}
