import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type {
  NutritionPlan,
  UpsertMealInput,
  UpsertMealItemInput,
  UpsertNutritionPlanInput,
} from '@/types/nutrition'

const TEMPLATES_KEY = ['admin-nutrition-templates']
const memberPlansKey = (memberId: string) => ['admin-member-nutrition-plans', memberId]

export function useNutritionTemplates() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: TEMPLATES_KEY,
    queryFn: () =>
      apiFetch<NutritionPlan[]>('/api/admin/nutrition-plan-templates', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useMemberNutritionPlans(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: memberPlansKey(memberId),
    queryFn: () =>
      apiFetch<NutritionPlan[]>(`/api/admin/members/${memberId}/nutrition-plans`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token),
  })
}

/** Every edit answers with the whole plan, so the two lists it can appear in are refetched. */
function useInvalidateNutrition() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY })
    queryClient.invalidateQueries({ queryKey: ['admin-member-nutrition-plans'] })
  }
}

export function useCreateNutritionTemplate() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: (input: UpsertNutritionPlanInput) =>
      apiFetch<NutritionPlan>('/api/admin/nutrition-plan-templates', {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: invalidate,
  })
}

export function useCreateMemberNutritionPlan(memberId: string) {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ input, templateId }: { input: UpsertNutritionPlanInput; templateId?: string }) =>
      apiFetch<NutritionPlan>(
        `/api/admin/members/${memberId}/nutrition-plans${templateId ? `?templateId=${templateId}` : ''}`,
        { method: 'POST', headers: adminHeaders(token!), body: JSON.stringify(input) },
      ),
    onSuccess: invalidate,
  })
}

export function useUpdateNutritionPlan() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ planId, input }: { planId: string; input: UpsertNutritionPlanInput }) =>
      apiFetch<NutritionPlan>(`/api/admin/nutrition-plans/${planId}`, {
        method: 'PUT',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: invalidate,
  })
}

export function useActivateNutritionPlan() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ planId }: { planId: string }) =>
      apiFetch<NutritionPlan>(`/api/admin/nutrition-plans/${planId}/activation`, {
        method: 'POST',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useDeleteNutritionPlan() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ planId }: { planId: string }) =>
      apiFetch<void>(`/api/admin/nutrition-plans/${planId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useAddMeal() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ planId, input }: { planId: string; input: UpsertMealInput }) =>
      apiFetch<NutritionPlan>(`/api/admin/nutrition-plans/${planId}/meals`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: invalidate,
  })
}

export function useDeleteMeal() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ mealId }: { mealId: string }) =>
      apiFetch<NutritionPlan>(`/api/admin/meals/${mealId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useAddMealItem() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ mealId, input }: { mealId: string; input: UpsertMealItemInput }) =>
      apiFetch<NutritionPlan>(`/api/admin/meals/${mealId}/items`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: invalidate,
  })
}

export function useDeleteMealItem() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidateNutrition()

  return useMutation({
    mutationFn: ({ itemId }: { itemId: string }) =>
      apiFetch<NutritionPlan>(`/api/admin/meal-items/${itemId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useMyNutritionPlan() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: ['member-nutrition-plan'],
    // 204 means "nothing assigned yet", which is not an error — but react-query rejects
    // `undefined` as data, so it is normalised to null.
    queryFn: () =>
      apiFetch<NutritionPlan | null>('/api/member/nutrition-plan', {
        headers: memberHeaders(session!.token),
      }).then((plan) => plan ?? null),
    enabled: Boolean(session),
  })
}
