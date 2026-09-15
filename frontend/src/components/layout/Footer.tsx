import type { MouseEvent } from 'react'

import { usePricingCategory } from '@/context/PricingCategoryContext'
import { isPlainLeftClick, navigateToSection } from '@/lib/scroll'
import type { PackageCategory } from '@/types/package'

interface FooterLink {
  label: string
  href: string
  targetId: string
}

const programs: (FooterLink & { category: PackageCategory })[] = [
  { label: 'Hipertrofi & Kas Kütlesi', href: '#paketler', targetId: 'paketler', category: 'KOCLUK' },
  { label: 'Yağ Yakımı & Definasyon', href: '#paketler', targetId: 'paketler', category: 'KOCLUK' },
  { label: 'Esnek Makro Beslenme', href: '#paketler-beslenme', targetId: 'paketler', category: 'BESLENME' },
  { label: 'Yarışma & Peak Week', href: '#paketler', targetId: 'paketler', category: 'KOCLUK' },
]
const platform: FooterLink[] = [
  { label: 'Nasıl Çalışır?', href: '#nasil-calisir', targetId: 'nasil-calisir' },
  { label: 'Başarı Hikayeleri', href: '#basari-hikayeleri', targetId: 'basari-hikayeleri' },
  { label: 'Sıkça Sorulan Sorular', href: '#sss', targetId: 'sss' },
]
const legal: FooterLink[] = [
  { label: 'Kullanım Koşulları', href: '#paketler', targetId: 'paketler' },
  { label: 'KVKK & Aydınlatma Metni', href: '#paketler', targetId: 'paketler' },
  { label: 'Mesafeli Satış Sözleşmesi', href: '#paketler', targetId: 'paketler' },
  { label: 'İptal ve İade Koşulları', href: '#paketler', targetId: 'paketler' },
]

export function Footer() {
  const { setCategory } = usePricingCategory()

  return (
    <footer className="w-full border-t border-border bg-surface py-16">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-10 border-b border-border pb-12 md:grid-cols-2 lg:grid-cols-4">
          <div className="flex flex-col">
            <div className="mb-3 flex items-center gap-2">
              <div className="flex h-7 w-7 items-center justify-center rounded border border-border bg-canvas text-xs font-bold text-primary">
                FC
              </div>
              <span className="font-display text-sm font-bold uppercase tracking-wider text-text-primary">
                S2000 Coaching
              </span>
            </div>
            <p className="mb-4 text-xs leading-relaxed text-text-secondary">
              Bilimsel hipertrofi, ileri seviye biyomekanik analiz ve esnek makro mimarisiyle tasarlanmış sporcu
              koçluk platformu.
            </p>
          </div>

          <FooterColumn
            title="Programlar"
            items={programs}
            onLinkClick={(event, program) => {
              if (!isPlainLeftClick(event)) return
              event.preventDefault()
              setCategory(program.category)
              navigateToSection(program.href, program.targetId)
            }}
          />
          <FooterColumn title="Platform" items={platform} />
          <FooterColumn title="Yasal & Gizlilik" items={legal} />
        </div>

        <div className="flex flex-col items-center justify-between gap-4 pt-8 text-xs text-text-muted sm:flex-row">
          <span>© 2026 S2000 Coaching. Tüm hakları saklıdır.</span>
          <div className="flex items-center gap-6">
            <a href="#" className="hover:text-text-primary transition-colors">
              Instagram
            </a>
            <a href="#" className="hover:text-text-primary transition-colors">
              YouTube
            </a>
            <a href="#" className="hover:text-text-primary transition-colors">
              WhatsApp
            </a>
          </div>
        </div>
      </div>
    </footer>
  )
}

function FooterColumn<T extends FooterLink>({
  title,
  items,
  onLinkClick,
}: {
  title: string
  items: T[]
  onLinkClick?: (event: MouseEvent<HTMLAnchorElement>, item: T) => void
}) {
  return (
    <div className="flex flex-col space-y-2.5">
      <div className="mb-1 text-xs font-semibold uppercase tracking-wider text-text-primary">{title}</div>
      {items.map((item) => (
        <a
          key={item.label}
          href={item.href}
          onClick={(event) => {
            if (onLinkClick) {
              onLinkClick(event, item)
            } else if (isPlainLeftClick(event)) {
              event.preventDefault()
              navigateToSection(item.href, item.targetId)
            }
          }}
          className="text-xs text-text-secondary transition-colors hover:text-text-primary"
        >
          {item.label}
        </a>
      ))}
    </div>
  )
}
