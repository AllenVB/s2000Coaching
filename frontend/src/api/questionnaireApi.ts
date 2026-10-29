import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Questionnaire, UpsertQuestionnaireInput } from '@/types/questionnaire'

const MEMBER_KEY = ['member-questionnaire']

export function useMyQuestionnaire() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () =>
      apiFetch<Questionnaire>('/api/member/questionnaire', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useSaveMyQuestionnaire() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpsertQuestionnaireInput) =>
      apiFetch<Questionnaire>('/api/member/questionnaire', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(input),
      }),
    onSuccess: (saved) => queryClient.setQueryData(MEMBER_KEY, saved),
  })
}

export function useMemberQuestionnaire(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-member-questionnaire', memberId],
    queryFn: () =>
      apiFetch<Questionnaire>(`/api/admin/members/${memberId}/questionnaire`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}
