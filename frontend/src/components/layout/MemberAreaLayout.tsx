import { useMemo } from 'react'
import { Navigate, Outlet } from 'react-router-dom'

import { useMemberLogout } from '@/api/memberAuthApi'
import { useMyUnreadMessages } from '@/api/messagesApi'
import { PortalHeader } from '@/components/layout/PortalHeader'
import { PortalSidebarNav, PortalTabBar, type PortalNavItem } from '@/components/layout/PortalNav'
import { useFlushPendingWorkouts } from '@/api/trainingApi'
import { clearCachedPlan } from '@/lib/offline'
import { useOnlineStatus } from '@/hooks/useOnlineStatus'
import { MemberAuthProvider, useMemberAuth } from '@/context/MemberAuthContext'
import { ChartIcon, ChatIcon, DumbbellIcon, HistoryIcon, UserIcon } from '@/components/ui/icons'

/** Five places, every one on the phone's tab bar too: nothing is hidden on a small screen. */
const navItems: PortalNavItem[] = [
  { to: '/uye/programlarim', label: 'Programlarım', icon: DumbbellIcon },
  { to: '/uye/olcumlerim', label: 'Ölçümlerim', icon: ChartIcon },
  { to: '/uye/gecmis', label: 'Geçmiş', icon: HistoryIcon },
  { to: '/uye/mesajlar', label: 'Mesajlar', icon: ChatIcon },
  { to: '/uye/profil', label: 'Profil', icon: UserIcon },
]

/** Shell for every `/uye/*` route: sign-in gate, then sidebar (desktop) or tab bar (phone). */
export function MemberAreaLayout() {
  return (
    <MemberAuthProvider>
      <MemberAreaShell />
    </MemberAuthProvider>
  )
}

function MemberAreaShell() {
  const { session, signOut } = useMemberAuth()
  const logout = useMemberLogout()
  const { data: unread } = useMyUnreadMessages()

  const items = useMemo(
    () =>
      navItems.map((item) =>
        item.to === '/uye/mesajlar' ? { ...item, badge: unread?.unread ?? 0 } : item,
      ),
    [unread],
  )
  useFlushPendingWorkouts()
  const online = useOnlineStatus()

  if (!session) {
    return <Navigate to="/giris" replace />
  }

  const handleLogout = () => {
    // Sign out locally regardless of the response: the stored token is useless to us either way.
    clearCachedPlan(session.member.id)
    logout.mutate({ token: session.token }, { onSettled: () => signOut() })
  }

  return (
    <>
      <PortalHeader
        eyebrow="Üye Alanı"
        title={session.member.fullName}
        action={
          <button
            type="button"
            onClick={handleLogout}
            disabled={logout.isPending}
            className="h-9 rounded-lg border border-border bg-surface px-3.5 text-xs font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-white/25 hover:text-text-primary disabled:opacity-50"
          >
            {logout.isPending ? 'Çıkılıyor…' : 'Çıkış Yap'}
          </button>
        }
      />

      {online ? null : (
        <p className="bg-warning/15 px-4 py-2 text-center print:hidden text-xs font-medium text-warning">
          Çevrimdışısınız — son yüklenen planınız gösteriliyor; kaydettiğiniz antrenmanlar bağlantı gelince gönderilir.
        </p>
      )}

      <div className="mx-auto flex w-full max-w-[1280px] gap-8 px-4 pb-24 pt-8 sm:px-6 lg:px-8 lg:pb-14 print:max-w-none print:p-0">
        <aside className="hidden w-56 shrink-0 lg:block print:hidden">
          <div className="sticky top-24 rounded-2xl border border-border bg-surface p-3">
            <PortalSidebarNav items={items} />
          </div>
        </aside>
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>

      <PortalTabBar items={items} />
    </>
  )
}
