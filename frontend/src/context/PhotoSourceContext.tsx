import { createContext, useContext, useMemo, type ReactNode } from 'react'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { memberHeaders } from '@/lib/memberSession'

/**
 * How the photo components fetch image bytes.
 *
 * <p>The gallery and the comparison are shared between the member area and the coach panel,
 * but the two sign in through different contexts — and `useAdminAuth` throws outside its
 * provider, so a shared component that reached for both would crash whichever side it was
 * not rendered under. Each side wraps its photo pages in the matching provider below, and
 * the components below that need to know nothing about who is signed in.
 */
interface PhotoSource {
  /** The endpoint serving one photo's bytes, and the headers that authorize it. */
  contentPath: (photoId: string) => string
  headers: Record<string, string> | null
}

const PhotoSourceContext = createContext<PhotoSource | null>(null)

export function MemberPhotoSourceProvider({ children }: { children: ReactNode }) {
  const { session } = useMemberAuth()

  const value = useMemo<PhotoSource>(
    () => ({
      contentPath: (photoId) => `/api/member/photos/${photoId}/content`,
      headers: session ? memberHeaders(session.token) : null,
    }),
    [session],
  )

  return <PhotoSourceContext.Provider value={value}>{children}</PhotoSourceContext.Provider>
}

export function CoachPhotoSourceProvider({ children }: { children: ReactNode }) {
  const { token } = useAdminAuth()

  const value = useMemo<PhotoSource>(
    () => ({
      contentPath: (photoId) => `/api/admin/photos/${photoId}/content`,
      headers: token ? adminHeaders(token) : null,
    }),
    [token],
  )

  return <PhotoSourceContext.Provider value={value}>{children}</PhotoSourceContext.Provider>
}

export function usePhotoSource(): PhotoSource {
  const value = useContext(PhotoSourceContext)
  if (!value) {
    throw new Error('usePhotoSource must be used within a photo source provider')
  }
  return value
}
