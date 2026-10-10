import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect } from 'react'

import { useMembers } from '@/api/adminMembersApi'
import { CreateMemberForm } from '@/components/admin/CreateMemberForm'
import { MemberList } from '@/components/admin/MemberList'
import { AdminTokenGate } from '@/components/admin/AdminTokenGate'
import { PortalHeader } from '@/components/layout/PortalHeader'
import { AdminAuthProvider, useAdminAuth } from '@/context/AdminAuthContext'
import { ApiError } from '@/lib/api'

/** Management area (`/yonetim`): the settings page for adding and removing members. */
export function AdminPage() {
  return (
    <AdminAuthProvider>
      <AdminPageContent />
    </AdminAuthProvider>
  )
}

function AdminPageContent() {
  const { token, signOut } = useAdminAuth()
  const queryClient = useQueryClient()

  const handleSignOut = useCallback(() => {
    signOut()
    queryClient.removeQueries({ queryKey: ['admin-members'] })
  }, [signOut, queryClient])

  return (
    <>
      <PortalHeader
        eyebrow="Yönetim"
        title="Üye Ayarları"
        action={
          token ? (
            <button
              type="button"
              onClick={handleSignOut}
              className="h-9 rounded-lg border border-border bg-surface px-3.5 text-xs font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-white/25 hover:text-text-primary"
            >
              Çıkış Yap
            </button>
          ) : null
        }
      />
      <main className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6 sm:py-14 lg:px-8">
        {token ? <MemberSettings onUnauthorized={handleSignOut} /> : <AdminTokenGate />}
      </main>
    </>
  )
}

function MemberSettings({ onUnauthorized }: { onUnauthorized: () => void }) {
  // Shares the cached query with MemberList; used here only to react to a rejected key
  // (rotated or revoked server-side) by returning to the key prompt.
  const { error } = useMembers()

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      onUnauthorized()
    }
  }, [error, onUnauthorized])

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary sm:text-3xl">
          Üye Yönetimi
        </h1>
        <p className="mt-2 max-w-2xl text-sm leading-relaxed text-text-secondary">
          Üye ekleyip çıkarabilir, bekleyen üyeliklerin onay kodunu doğrulayabilirsiniz. Ekleme ve silme
          işlemleri onay istenerek yapılır.
        </p>
      </div>
      <CreateMemberForm />
      <MemberList />
    </div>
  )
}
