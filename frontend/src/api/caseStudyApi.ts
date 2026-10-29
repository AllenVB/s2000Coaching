import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch, apiUpload } from '@/lib/api'
import type { PhotoPose } from '@/types/member'
import type {
  BodyAnalysis,
  CaseStudy,
  CaseStudyPhoto,
  CsvPreview,
  DataKind,
  ImportResult,
  PeriodPhotos,
  PublicCaseStudy,
  PublicCaseStudySummary,
  UpsertCaseStudyInput,
} from '@/types/caseStudy'

const ADMIN_KEY = ['admin-case-studies']
const PUBLIC_KEY = ['case-studies']

const analysisKey = (id: string) => ['admin-case-study-analysis', id]

// --- public ----------------------------------------------------------------------------

export function usePublishedCaseStudies() {
  return useQuery({
    queryKey: PUBLIC_KEY,
    queryFn: () => apiFetch<PublicCaseStudySummary[]>('/api/case-studies'),
    staleTime: 60 * 1000,
  })
}

export function usePublicCaseStudy(slug: string | null) {
  return useQuery({
    queryKey: [...PUBLIC_KEY, slug],
    queryFn: () => apiFetch<PublicCaseStudy>(`/api/case-studies/${slug}`),
    enabled: Boolean(slug),
    staleTime: 60 * 1000,
  })
}

// --- coach -----------------------------------------------------------------------------

export function useAdminCaseStudies() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ADMIN_KEY,
    queryFn: () => apiFetch<CaseStudy[]>('/api/admin/case-studies', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

/**
 * The analysis as the public page would render it. Kept out of the cache for longer than a
 * moment on purpose: it is the thing the coach checks after an import, so a stale copy
 * would show the previous upload's numbers.
 */
export function useCaseStudyAnalysis(id: string | null) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: analysisKey(id ?? ''),
    queryFn: () =>
      apiFetch<BodyAnalysis>(`/api/admin/case-studies/${id}/analysis`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token && id),
    staleTime: 0,
  })
}

export function useCreateCaseStudy() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpsertCaseStudyInput) =>
      apiFetch<CaseStudy>('/api/admin/case-studies', {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: (created) => {
      queryClient.setQueryData<CaseStudy[]>(ADMIN_KEY, (old) => [created, ...(old ?? [])])
    },
  })
}

export function useUpdateCaseStudy() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: UpsertCaseStudyInput }) =>
      apiFetch<CaseStudy>(`/api/admin/case-studies/${id}`, {
        method: 'PUT',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: (updated) => replaceInList(queryClient, updated),
  })
}

export function useSetCaseStudyPublished() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, published }: { id: string; published: boolean }) =>
      apiFetch<CaseStudy>(
        `/api/admin/case-studies/${id}/${published ? 'publish' : 'unpublish'}`,
        { method: 'POST', headers: adminHeaders(token!) },
      ),
    onSuccess: (updated) => {
      replaceInList(queryClient, updated)
      // The public list and page are now different; drop both rather than guess.
      queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
    },
  })
}

export function useDeleteCaseStudy() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/admin/case-studies/${id}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: (_result, id) => {
      queryClient.setQueryData<CaseStudy[]>(ADMIN_KEY, (old) =>
        (old ?? []).filter((study) => study.id !== id),
      )
      queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
    },
  })
}

/** Reads the file and returns the guessed mapping. Writes nothing. */
export function usePreviewCsv() {
  const { token } = useAdminAuth()

  return useMutation({
    mutationFn: ({ id, kind, file }: { id: string; kind: DataKind; file: File }) => {
      const body = new FormData()
      body.append('kind', kind)
      body.append('file', file)
      return apiUpload<CsvPreview>(
        `/api/admin/case-studies/${id}/preview`,
        body,
        adminHeaders(token!),
      )
    },
  })
}

export function useImportCsv() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      id,
      kind,
      file,
      mapping,
    }: {
      id: string
      kind: DataKind
      file: File
      mapping: Record<string, number>
    }) => {
      const body = new FormData()
      body.append('kind', kind)
      body.append('file', file)
      body.append('mapping', JSON.stringify(mapping))
      return apiUpload<ImportResult>(
        `/api/admin/case-studies/${id}/import`,
        body,
        adminHeaders(token!),
      )
    },
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({ queryKey: ADMIN_KEY })
      queryClient.invalidateQueries({ queryKey: analysisKey(variables.id) })
      queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
    },
  })
}

export function useClearCaseStudyData() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, kind }: { id: string; kind: DataKind }) =>
      apiFetch<void>(`/api/admin/case-studies/${id}/data?kind=${kind}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: (_result, variables) => {
      queryClient.invalidateQueries({ queryKey: ADMIN_KEY })
      queryClient.invalidateQueries({ queryKey: analysisKey(variables.id) })
      queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
    },
  })
}

function replaceInList(
  queryClient: ReturnType<typeof useQueryClient>,
  updated: CaseStudy,
) {
  queryClient.setQueryData<CaseStudy[]>(ADMIN_KEY, (old) =>
    (old ?? []).map((study) => (study.id === updated.id ? updated : study)),
  )
}

// --- photos (coach) --------------------------------------------------------------------

const photosKey = (id: string) => ['admin-case-study-photos', id]
const photoPeriodsKey = (id: string) => ['admin-case-study-photo-periods', id]

export function useCaseStudyPhotos(id: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: photosKey(id),
    queryFn: () =>
      apiFetch<CaseStudyPhoto[]>(`/api/admin/case-studies/${id}/photos`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token),
  })
}

/** The pairing exactly as the public page would show it, before photos are switched on. */
export function useCaseStudyPhotoPeriods(id: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: photoPeriodsKey(id),
    queryFn: () =>
      apiFetch<PeriodPhotos[]>(`/api/admin/case-studies/${id}/photo-periods`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token),
    staleTime: 0,
  })
}

function invalidatePhotos(queryClient: ReturnType<typeof useQueryClient>, id: string) {
  queryClient.invalidateQueries({ queryKey: photosKey(id) })
  queryClient.invalidateQueries({ queryKey: photoPeriodsKey(id) })
  queryClient.invalidateQueries({ queryKey: ADMIN_KEY })
  queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
}

export function useUploadCaseStudyPhoto(id: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ file, takenOn, pose }: { file: File; takenOn: string; pose: PhotoPose }) => {
      const body = new FormData()
      body.append('file', file)
      body.append('takenOn', takenOn)
      body.append('pose', pose)
      return apiUpload<CaseStudyPhoto>(
        `/api/admin/case-studies/${id}/photos`,
        body,
        adminHeaders(token!),
      )
    },
    onSuccess: () => invalidatePhotos(queryClient, id),
  })
}

export function useDeleteCaseStudyPhoto(id: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (photoId: string) =>
      apiFetch<void>(`/api/admin/case-study-photos/${photoId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: () => invalidatePhotos(queryClient, id),
  })
}

/** Its own switch, apart from publishing the data; turning it on needs an explicit confirmation. */
export function useSetCaseStudyPhotosPublished(id: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (published: boolean) =>
      apiFetch<CaseStudy>(
        `/api/admin/case-studies/${id}/photos/${published ? 'publish' : 'unpublish'}`,
        {
          method: 'POST',
          headers: adminHeaders(token!),
          ...(published ? { body: JSON.stringify({ confirmed: true }) } : {}),
        },
      ),
    onSuccess: (updated) => {
      replaceInList(queryClient, updated)
      queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
    },
  })
}
