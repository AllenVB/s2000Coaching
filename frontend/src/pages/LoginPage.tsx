import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { AdminTokenGate } from '@/components/admin/AdminTokenGate'
import { MemberLogin } from '@/components/member/MemberLogin'
import { AdminAuthProvider, useAdminAuth } from '@/context/AdminAuthContext'
import { MemberAuthProvider, useMemberAuth } from '@/context/MemberAuthContext'

type Area = { name: 'member'; start: 'login' | 'register' } | { name: 'admin' }

/**
 * The one way in from the site: members sign in or open an account, the coach signs in to
 * management. Each side keeps its own session and its own area; this page only decides where
 * the person goes once they are in. Someone who is already signed in is sent straight on.
 */
export function LoginPage() {
  return (
    <AdminAuthProvider>
      <MemberAuthProvider>
        <LoginScreen />
      </MemberAuthProvider>
    </AdminAuthProvider>
  )
}

function LoginScreen() {
  const navigate = useNavigate()
  const { session: coachSession } = useAdminAuth()
  const { session: memberSession } = useMemberAuth()
  const [area, setArea] = useState<Area>({ name: 'member', start: 'login' })

  // A coach who signs in lands in management; a member in their own area. Both sessions can
  // exist in one browser (the coach checking how a member sees things), and management wins.
  useEffect(() => {
    if (coachSession) {
      navigate('/yonetim', { replace: true })
    } else if (memberSession) {
      navigate('/uye', { replace: true })
    }
  }, [coachSession, memberSession, navigate])

  return (
    <div className="min-h-screen">
      <header className="w-full border-b border-border bg-canvas/90">
        <div className="mx-auto flex min-h-[72px] max-w-[1280px] items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
          <Link to="/" className="flex items-center gap-3" aria-label="Ana sayfaya dön">
            <span className="flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-surface font-display text-sm font-black tracking-wider text-primary">
              FC
            </span>
            <span className="flex flex-col leading-none">
              <span className="font-display text-base font-bold uppercase tracking-wider text-text-primary">S2000</span>
              <span className="mt-0.5 text-[10px] font-semibold uppercase tracking-[0.22em] text-primary">Coaching</span>
            </span>
          </Link>
          <Link to="/" className="text-xs font-medium text-text-secondary transition-colors hover:text-text-primary">
            Siteye dön
          </Link>
        </div>
      </header>

      <main className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6 sm:py-14 lg:px-8">
        {area.name === 'admin' ? (
          <AdminTokenGate onMember={(start) => setArea({ name: 'member', start })} />
        ) : (
          // `key` restarts the form when the tab changes, so "Hesap Oluştur" opens on the sign-up step.
          <MemberLogin key={area.start} initialStep={area.start} onAdmin={() => setArea({ name: 'admin' })} />
        )}
      </main>
    </div>
  )
}
