import { Link } from 'react-router-dom'

import { ChatIcon } from '@/components/ui/icons'

/**
 * The questions members actually ask between sessions. Each one opens the thread with the
 * question already typed, so getting hold of the coach is one tap rather than a blank page.
 */
const quickQuestions = [
  'Bu hafta antrenman programımla ilgili bir sorum var:',
  'Beslenme planımda şunu değiştirmem gerekiyor:',
  'Bu hafta antrenmana gidemedim, nasıl telafi edeyim?',
  'Bir sakatlığım/ağrım var:',
]

export function AskCoachCard({ unread = 0 }: { unread?: number }) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <ChatIcon className="h-5 w-5 text-primary" />
          <h2 className="text-base font-semibold text-text-primary">Koçuna Sor</h2>
        </div>
        {unread > 0 ? (
          <Link
            to="/uye/mesajlar"
            className="rounded-full bg-primary px-3 py-1 text-[11px] font-bold uppercase tracking-wide text-canvas"
          >
            {unread} yeni mesaj
          </Link>
        ) : null}
      </div>
      <p className="mt-2 text-sm text-text-secondary">
        Aklına takılan bir şey mi var? Aşağıdakilerden birine dokun, mesajın hazır gelsin.
      </p>
      <ul className="mt-4 flex flex-wrap gap-2">
        {quickQuestions.map((question) => (
          <li key={question}>
            <Link
              to={`/uye/mesajlar?soru=${encodeURIComponent(question)}`}
              className="inline-flex rounded-full border border-border bg-surface-elevated px-3.5 py-1.5 text-xs font-semibold text-text-secondary transition-colors hover:border-primary/60 hover:text-text-primary"
            >
              {question}
            </Link>
          </li>
        ))}
      </ul>
      <Link
        to="/uye/mesajlar"
        className="mt-4 inline-flex text-xs font-semibold uppercase tracking-wide text-primary hover:underline"
      >
        Yazışmanın tamamını aç
      </Link>
    </section>
  )
}
