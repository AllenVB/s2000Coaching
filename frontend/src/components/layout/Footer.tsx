import { usePricingCategory } from '@/context/PricingCategoryContext'
import type { PackageCategory } from '@/types/package'

const programs: { label: string; category: PackageCategory }[] = [
  { label: 'Hipertrofi & Kas Kütlesi', category: 'KOCLUK' },
  { label: 'Yağ Yakımı & Definasyon', category: 'KOCLUK' },
  { label: 'Esnek Makro Beslenme', category: 'BESLENME' },
  { label: 'Yarışma & Peak Week', category: 'KOCLUK' },
]
const platform = ['Nasıl Çalışır?', 'Başarı Hikayeleri', 'Sıkça Sorulan Sorular']
const legal = ['Kullanım Koşulları', 'KVKK & Aydınlatma Metni', 'Mesafeli Satış Sözleşmesi', 'İptal ve İade Koşulları']

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

          <div className="flex flex-col space-y-2.5">
            <div className="mb-1 text-xs font-semibold uppercase tracking-wider text-text-primary">Programlar</div>
            {programs.map((program) => (
              <a
                key={program.label}
                href="#paketler"
                onClick={() => setCategory(program.category)}
                className="text-xs text-text-secondary transition-colors hover:text-text-primary"
              >
                {program.label}
              </a>
            ))}
          </div>

          <FooterColumn title="Platform" items={platform} hrefs={['#nasil-calisir', '#basari-hikayeleri', '#sss']} />
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

function FooterColumn({ title, items, hrefs }: { title: string; items: string[]; hrefs?: string[] }) {
  return (
    <div className="flex flex-col space-y-2.5">
      <div className="mb-1 text-xs font-semibold uppercase tracking-wider text-text-primary">{title}</div>
      {items.map((item, index) => (
        <a
          key={item}
          href={hrefs?.[index] ?? '#paketler'}
          className="text-xs text-text-secondary transition-colors hover:text-text-primary"
        >
          {item}
        </a>
      ))}
    </div>
  )
}
