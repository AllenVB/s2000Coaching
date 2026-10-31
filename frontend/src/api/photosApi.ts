import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'

import type { PhotoPose, ProgressPhoto } from '@/types/member'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { usePhotoSource } from '@/context/PhotoSourceContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { apiFetch, apiFetchObjectUrl, apiUpload } from '@/lib/api'
import { adminHeaders } from '@/lib/adminSession'
import { memberHeaders } from '@/lib/memberSession'

const MEMBER_KEY = ['member-photos']
const adminKey = (memberId: string) => ['admin-member-photos', memberId]

/* ---------- member side ---------- */

export function useMyPhotos() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () =>
      apiFetch<ProgressPhoto[]>('/api/member/photos', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export interface UploadPhotoInput {
  file: File
  takenOn: string
  pose: PhotoPose
  note?: string
}

export function useUploadMyPhoto() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ file, takenOn, pose, note }: UploadPhotoInput) => {
      const body = new FormData()
      body.append('file', file)
      body.append('takenOn', takenOn)
      body.append('pose', pose)
      if (note) {
        body.append('note', note)
      }
      return apiUpload<ProgressPhoto>('/api/member/photos', body, memberHeaders(session!.token))
    },
    onSuccess: (photo) => {
      queryClient.setQueryData<ProgressPhoto[]>(MEMBER_KEY, (old) =>
        [photo, ...(old ?? [])].sort((a, b) => b.takenOn.localeCompare(a.takenOn)),
      )
    },
  })
}

/**
 * The image bytes as an object URL, released when the component goes away.
 *
 * <p>Where the bytes come from is decided by `PhotoSourceContext`, so the same component
 * works in the member area and the coach panel without knowing which one it is in.
 *
 * <p>Not a react-query entry on purpose: an object URL has to be revoked by hand, and a
 * cached one would be dropped by the cache with nothing left to revoke it — a blob leaked
 * for the life of the tab. Refetching on remount costs nothing, because the endpoint sends
 * `private, max-age=3600` and the browser answers the second request from its own cache.
 */
export function usePhotoObjectUrl(photoId: string | null) {
  const { contentPath, headers } = usePhotoSource()
  const [url, setUrl] = useState<string | null>(null)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (!photoId || !headers) {
      return
    }

    let objectUrl: string | null = null
    // The id can change while a fetch is in flight; without this the slower response would
    // win and show the wrong photo.
    let cancelled = false

    apiFetchObjectUrl(contentPath(photoId), headers)
      .then((created) => {
        if (cancelled) {
          URL.revokeObjectURL(created)
          return
        }
        objectUrl = created
        setUrl(created)
      })
      .catch((cause: Error) => {
        if (!cancelled) {
          setError(cause)
        }
      })

    return () => {
      cancelled = true
      // React runs this before the next effect, so clearing here is also what resets the
      // state when the photo id changes — no synchronous setState in the effect body.
      setUrl(null)
      setError(null)
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [photoId, contentPath, headers])

  return { url, error }
}

/* ---------- coach side ---------- */

export function useMemberPhotos(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<ProgressPhoto[]>(`/api/admin/members/${memberId}/photos`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token),
  })
}

export function useDeleteMemberPhoto(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ photoId }: { photoId: string }) =>
      apiFetch<void>(`/api/admin/photos/${photoId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: (_result, { photoId }) => {
      queryClient.setQueryData<ProgressPhoto[]>(adminKey(memberId), (old) =>
        old?.filter((photo) => photo.id !== photoId),
      )
    },
  })
}
