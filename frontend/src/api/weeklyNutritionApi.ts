import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch, apiFetchObjectUrl, apiUpload } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { SaveWeeklyNutritionInput, WeeklyNutrition } from '@/types/weeklyNutrition'

/** What the album needs, whoever is looking: the member (who can also save) or the coach. */
export interface WeeklyNutritionSource {
  entries: WeeklyNutrition[] | undefined
  isLoading: boolean
  error: Error | null
  /** Absent for the coach, who reads the album but does not fill it in. */
  save?: (input: SaveWeeklyNutritionInput) => Promise<WeeklyNutrition>
  remove: (id: string) => Promise<void>
  /** Where one week's photo bytes are served, and the headers that authorise it. */
  photoPath: (entry: WeeklyNutrition) => string
  headers: Record<string, string> | null
}

/** The version in the address keeps the browser from showing an old photo after it was replaced. */
const photoUrl = (base: string) => (entry: WeeklyNutrition) =>
  `${base}/${entry.id}/photo?v=${encodeURIComponent(entry.updatedAt)}`

function toFormData(input: SaveWeeklyNutritionInput): FormData {
  const body = new FormData()
  body.append('weekStart', input.weekStart)
  body.append('calories', String(input.calories))
  body.append('carbsPercent', String(input.carbsPercent))
  body.append('fatPercent', String(input.fatPercent))
  body.append('proteinPercent', String(input.proteinPercent))
  if (input.photo) {
    body.append('photo', input.photo)
  }
  if (input.removePhoto) {
    body.append('removePhoto', 'true')
  }
  return body
}

export function useMemberWeeklyNutrition(): WeeklyNutritionSource {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()
  const key = ['member-weekly-nutrition']
  const refresh = () => queryClient.invalidateQueries({ queryKey: key })
  // Stable between renders, so the photo hook below does not refetch on every one.
  const headers = useMemo(() => (session ? memberHeaders(session.token) : null), [session])

  const list = useQuery({
    queryKey: key,
    queryFn: () =>
      apiFetch<WeeklyNutrition[]>('/api/member/weekly-nutrition', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
  const save = useMutation({
    mutationFn: (input: SaveWeeklyNutritionInput) =>
      apiUpload<WeeklyNutrition>('/api/member/weekly-nutrition', toFormData(input), memberHeaders(session!.token), 'PUT'),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/member/weekly-nutrition/${id}`, { method: 'DELETE', headers: memberHeaders(session!.token) }),
    onSuccess: refresh,
  })

  return {
    entries: list.data,
    isLoading: list.isLoading,
    error: list.error,
    save: save.mutateAsync,
    remove: remove.mutateAsync,
    photoPath: photoUrl('/api/member/weekly-nutrition'),
    headers,
  }
}

export function useCoachWeeklyNutrition(memberId: string): WeeklyNutritionSource {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()
  const key = ['admin-weekly-nutrition', memberId]
  const headers = useMemo(() => (token ? adminHeaders(token) : null), [token])

  const list = useQuery({
    queryKey: key,
    queryFn: () =>
      apiFetch<WeeklyNutrition[]>(`/api/admin/members/${memberId}/weekly-nutrition`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
  const remove = useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/admin/weekly-nutrition/${id}`, { method: 'DELETE', headers: adminHeaders(token!) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key }),
  })

  return {
    entries: list.data,
    isLoading: list.isLoading,
    error: list.error,
    remove: remove.mutateAsync,
    photoPath: photoUrl('/api/admin/weekly-nutrition'),
    headers,
  }
}

/** A photo behind a bearer token: fetched as bytes, shown from an object URL that is revoked on the way out. */
export function useAuthedImage(path: string, headers: Record<string, string> | null) {
  const [url, setUrl] = useState<string | null>(null)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    if (!headers) {
      return
    }
    let objectUrl: string | null = null
    let cancelled = false
    apiFetchObjectUrl(path, headers)
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
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [path, headers])

  return { url, error }
}
