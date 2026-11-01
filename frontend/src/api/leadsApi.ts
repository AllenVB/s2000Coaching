import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'

export type LeadStatus = 'NEW' | 'CONTACTED'

export interface Lead {
  id: string
  fullName: string
  phone: string
  email: string | null
  goal: string
  level: string | null
  heightCm: number | null
  weightKg: number | null
  note: string | null
  status: LeadStatus
  createdAt: string
}

export interface CreateLeadInput {
  fullName: string
  phone: string
  email: string
  goal: string
  level: string
  heightCm: number | null
  weightKg: number | null
  note: string
  consent: boolean
  /** Honeypot: stays empty for people. */
  website: string
}

export function useSubmitLead() {
  return useMutation({
    mutationFn: (input: CreateLeadInput) =>
      apiFetch<void>('/api/leads', { method: 'POST', body: JSON.stringify(input) }),
  })
}

const KEY = ['admin-leads']

export function useLeads() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: KEY,
    queryFn: () => apiFetch<Lead[]>('/api/admin/leads', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
    refetchInterval: 60 * 1000,
  })
}

export function useSetLeadStatus() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: LeadStatus }) =>
      apiFetch<Lead>(`/api/admin/leads/${id}/status?status=${status}`, { method: 'PUT', headers: adminHeaders(token!) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}

export function useDeleteLead() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/admin/leads/${id}`, { method: 'DELETE', headers: adminHeaders(token!) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}
