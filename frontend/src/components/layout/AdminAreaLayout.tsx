import { useMemo } from 'react'
import { Navigate, Outlet } from 'react-router-dom'

import { useLeads } from '@/api/leadsApi'
import { useCoachUnreadMessages } from '@/api/messagesApi'

import { PortalHeader } from '@/components/layout/PortalHeader'
import { PortalSidebarNav, PortalTabBar, type PortalNavItem } from '@/components/layout/PortalNav'
import { AdminAuthProvider, useAdminAuth } from '@/context/AdminAuthContext'
import { useAdminSignOut } from '@/hooks/useAdminSession'
import {
  ChartIcon,
  ChatIcon,
  ClipboardIcon,
  DumbbellIcon,
  HomeIcon,
  ReceiptIcon,
  SettingsIcon,
  StarIcon,
  UsersIcon,
} from '@/components/ui/icons'

const navItems: PortalNavItem[] = [
  { to: '/yonetim', label: 'Panel', icon: HomeIcon, end: true },
  { to: '/yonetim/uyeler', label: 'Üyeler', icon: UsersIcon },
  { to: '/yonetim/mesajlar', label: 'Mesajlar', icon: ChatIcon },
  { to: '/yonetim/programlar', label: 'Programlar', icon: DumbbellIcon },
  { to: '/yonetim/uye-verileri', label: 'Üye Verileri', icon: ChartIcon },
  { to: '/yonetim/sablonlar', label: 'Şablonlar', icon: ClipboardIcon },
  { to: '/yonetim/hareketler', label: 'Hareketler', icon: DumbbellIcon, desktopOnly: true },
  { to: '/yonetim/vaka-calismalari', label: 'Vakalar', icon: ChartIcon, desktopOnly: true },
  { to: '/yonetim/hikayeler', label: 'Hikâyeler', icon: StarIcon, desktopOnly: true },
  { to: '/yonetim/tecrubelerim', label: 'Tecrübelerim', icon: ClipboardIcon, desktopOnly: true },
  { to: '/yonetim/basvurular', label: 'Başvurular', icon: UsersIcon },
  { to: '/yonetim/siparisler', label: 'Siparişler', icon: ReceiptIcon },
  { to: '/yonetim/kodlar', label: 'Kodlar', icon: ReceiptIcon, desktopOnly: true },
  { to: '/yonetim/ayarlar', label: 'Ayarlar', icon: SettingsIcon },
]

/** Shell for every `/yonetim/*` route: management-key gate, then the section navigation. */
export function AdminAreaLayout() {
  return (
    <AdminAuthProvider>
      <AdminAreaShell />
    </AdminAuthProvider>
  )
}

function AdminAreaShell() {
  const { session, token } = useAdminAuth()
  const handleSignOut = useAdminSignOut()
  const { data: unread } = useCoachUnreadMessages()
  const { data: leads } = useLeads()
  const newLeads = leads?.filter((lead) => lead.status === 'NEW').length ?? 0

  const items = useMemo(
    () =>
      navItems.map((item) =>
        item.to === '/yonetim/mesajlar'
          ? { ...item, badge: unread?.unread ?? 0 }
          : item.to === '/yonetim/basvurular'
            ? { ...item, badge: newLeads }
            : item,
      ),
    [unread, newLeads],
  )

  // There is one sign-in page for everybody; management has no entrance of its own.
  if (!token) {
    return <Navigate to="/giris" replace />
  }

  return (
    <>
      <PortalHeader
        eyebrow="Yönetim"
        title={session?.coach.fullName ?? 'Koç Paneli'}
        action={
          <button
            type="button"
            onClick={handleSignOut}
            className="h-9 rounded-lg border border-border bg-surface px-3.5 text-xs font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-white/25 hover:text-text-primary"
          >
            Çıkış Yap
          </button>
        }
      />

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
