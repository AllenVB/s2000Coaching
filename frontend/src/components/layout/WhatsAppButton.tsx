import { usePublicSite } from '@/api/siteApi'

/** WhatsApp has no brand glyph in the icon set, so the bubble is drawn here. */
function WhatsAppGlyph() {
  return (
    <svg viewBox="0 0 24 24" className="h-7 w-7" fill="currentColor" aria-hidden="true">
      <path d="M12.04 2a9.9 9.9 0 0 0-8.5 14.9L2 22l5.25-1.5A9.9 9.9 0 1 0 12.04 2Zm0 1.8a8.1 8.1 0 1 1-4.3 14.98l-.3-.19-3.1.9.93-3.02-.2-.31A8.1 8.1 0 0 1 12.04 3.8Zm-3.2 3.9c-.2 0-.5.07-.77.37-.26.3-1 1-1 2.43s1.03 2.82 1.17 3.01c.15.2 2 3.2 4.95 4.36 2.45.96 2.95.77 3.48.72.53-.05 1.7-.7 1.94-1.37.24-.67.24-1.25.17-1.37-.07-.12-.26-.2-.55-.34-.3-.15-1.7-.84-1.97-.94-.26-.1-.46-.15-.65.15-.2.3-.75.94-.92 1.13-.17.2-.34.22-.63.07-.3-.15-1.24-.46-2.37-1.46-.88-.78-1.47-1.75-1.64-2.05-.17-.3-.02-.46.13-.6.13-.13.3-.34.44-.51.15-.17.2-.3.3-.5.1-.2.05-.37-.02-.52-.07-.15-.65-1.6-.9-2.18-.23-.56-.47-.49-.65-.5h-.55Z" />
    </svg>
  )
}

const GREETING = 'Merhaba, koçluk hakkında bilgi almak istiyorum.'

/**
 * A floating WhatsApp button on the public pages. The number is whatever the coach wrote under
 * Ayarlar → Sosyal Bağlantılar; with none set there is no button, because a button that goes nowhere
 * is worse than none. It opens a chat with a first message already typed.
 */
export function WhatsAppButton() {
  const { data: site } = usePublicSite()
  const link = site?.social.whatsapp
  if (!link) {
    return null
  }

  let href = link
  try {
    const url = new URL(link)
    url.searchParams.set('text', GREETING)
    href = url.toString()
  } catch {
    // An address that does not parse is left as stored.
  }

  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      aria-label="WhatsApp ile yazın"
      className="fixed bottom-5 right-5 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-[#25D366] text-white shadow-[0_10px_30px_-8px_rgba(37,211,102,0.7)] transition-transform hover:scale-105 print:hidden"
    >
      <WhatsAppGlyph />
    </a>
  )
}
