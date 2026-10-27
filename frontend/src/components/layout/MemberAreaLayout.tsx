import { Outlet } from 'react-router-dom'

import { useMemberLogout } from '@/api/memberAuthApi'
import { PortalHeader } from '@/components/layout/PortalHeader'
import { PortalSidebarNav, PortalTabBar, type PortalNavItem } from '@/components/layout/PortalNav'
import { MemberLogin } from '@/components/member/MemberLogin'
import { MemberAuthProvider, useMemberAuth } from '@/context/MemberAuthContext'
import {
  AppleIcon,
  ChartIcon,
  ChatIcon,
  CameraIcon,
  ClipboardIcon,
  DumbbellIcon,
  HistoryIcon,
  HomeIcon,
  PlayIcon,
  StarIcon,
  UserIcon,
} from '@/components/ui/icons'

const navItems: PortalNavItem[] = [
  { to: '/uye', label: 'Bugün', icon: HomeIcon, end: true },
  { to: '/uye/antrenman', label: 'Antrenman', icon: DumbbellIcon },
  { to: '/uye/beslenme', label: 'Beslenme', icon: AppleIcon },
  { to: '/uye/olcumler', label: 'Ölçümler', icon: ChartIcon },
  { to: '/uye/haftalik', label: 'Haftalık', icon: ClipboardIcon, desktopOnly: true },
  { to: '/uye/form-kontrol', label: 'Form', icon: PlayIcon, desktopOnly: true },
  { to: '/uye/hikayem', label: 'Hikâyem', icon: StarIcon, desktopOnly: true },
  { to: '/uye/gecmis', label: 'Geçmiş', icon: HistoryIcon, desktopOnly: true },
  { to: '/uye/fotograflar', label: 'Fotoğraflar', icon: CameraIcon, desktopOnly: true },
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

  if (!session) {
    return (
      <>
        <PortalHeader eyebrow="Üye Alanı" title="S2000 Coaching" />
        <main className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6 sm:py-14 lg:px-8">
          <MemberLogin />
        </main>
      </>
    )
  }

  const handleLogout = () => {
    // Sign out locally regardless of the response: the stored token is useless to us either way.
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

      <div className="mx-auto flex w-full max-w-[1280px] gap-8 px-4 pb-24 pt-8 sm:px-6 lg:px-8 lg:pb-14">
        <aside className="hidden w-56 shrink-0 lg:block">
          <div className="sticky top-24 rounded-2xl border border-border bg-surface p-3">
            <PortalSidebarNav items={navItems} />
          </div>
        </aside>
        <main className="min-w-0 flex-1">
          <Outlet />
        </main>
      </div>

      <PortalTabBar items={navItems} />
    </>
  )
}
