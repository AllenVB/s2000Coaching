import { useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'

import { PortalHeader } from '@/components/layout/PortalHeader'
import { MemberDashboard } from '@/components/member/MemberDashboard'
import { MemberLogin } from '@/components/member/MemberLogin'
import { MemberAuthProvider, useMemberAuth } from '@/context/MemberAuthContext'

/** Member area (`/uye`): sign-in by e-mail code, then the member's own plans and profile. */
export function MemberPage() {
  return (
    <MemberAuthProvider>
      <MemberPageContent />
    </MemberAuthProvider>
  )
}

function MemberPageContent() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  // Drop the cached profile whenever the session goes away, so the next member to sign in
  // on this device never sees the previous one's data.
  useEffect(() => {
    if (!session) {
      queryClient.removeQueries({ queryKey: ['member-profile'] })
    }
  }, [session, queryClient])

  return (
    <>
      <PortalHeader eyebrow="Üye Alanı" title="S2000 Coaching" />
      <main className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6 sm:py-14 lg:px-8">
        {session ? <MemberDashboard /> : <MemberLogin />}
      </main>
    </>
  )
}
