import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import type { AdminSiteSettings, LegalDocument, SiteSettings, SiteSocial, SiteStat } from '@/types/site'

/** The public site's editable facts. Visitors never need a session, and a failure must not break the page. */
export function usePublicSite() {
  return useQuery({
    queryKey: ['site'],
    queryFn: () => apiFetch<SiteSettings>('/api/site'),
    staleTime: 5 * 60 * 1000,
    retry: false,
  })
}

export function useLegalDocument(slug: string) {
  return useQuery({
    queryKey: ['site-legal', slug],
    queryFn: () => apiFetch<LegalDocument>(`/api/site/legal/${slug}`),
    retry: false,
  })
}

const ADMIN_KEY = ['admin-site-settings']

export function useAdminSiteSettings() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ADMIN_KEY,
    queryFn: () => apiFetch<AdminSiteSettings>('/api/admin/site-settings', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

function useSiteUpdate<Input>(path: (input: Input) => string, body: (input: Input) => unknown) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: Input) =>
      apiFetch<AdminSiteSettings>(path(input), {
        method: 'PUT',
        headers: { ...adminHeaders(token!), 'Content-Type': 'application/json' },
        body: JSON.stringify(body(input)),
      }),
    onSuccess: (saved) => {
      queryClient.setQueryData(ADMIN_KEY, saved)
      // What visitors see changes with it.
      void queryClient.invalidateQueries({ queryKey: ['site'] })
      void queryClient.invalidateQueries({ queryKey: ['site-legal'] })
    },
  })
}

export function useUpdateSocial() {
  return useSiteUpdate<SiteSocial>(
    () => '/api/admin/site-settings/social',
    (input) => ({ instagram: input.instagram ?? '', youtube: input.youtube ?? '', whatsapp: input.whatsapp ?? '' }),
  )
}

/** `stats: null` puts the built-in figures back; an empty list shows none. */
export function useUpdateStats() {
  return useSiteUpdate<{ stats: SiteStat[] | null }>(
    () => '/api/admin/site-settings/stats',
    (input) => ({ stats: input.stats }),
  )
}

export function useUpdateLegal() {
  return useSiteUpdate<{ slug: string; content: string }>(
    (input) => `/api/admin/site-settings/legal/${input.slug}`,
    (input) => ({ content: input.content }),
  )
}
