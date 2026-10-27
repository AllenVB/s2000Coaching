import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type {
  Exercise,
  SaveWorkoutLogInput,
  TrainingStats,
  TrainingPlan,
  UpsertExerciseInput,
  UpsertPlanExerciseInput,
  UpsertPlanInput,
  WorkoutLog,
} from '@/types/training'

const EXERCISES_KEY = ['admin-exercises']
const TEMPLATES_KEY = ['admin-plan-templates']
const memberPlansKey = (memberId: string) => ['admin-member-plans', memberId]

/* ---------- exercise catalogue ---------- */

export function useExercises() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: EXERCISES_KEY,
    queryFn: () => apiFetch<Exercise[]>('/api/admin/exercises', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useCreateExercise() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpsertExerciseInput) =>
      apiFetch<Exercise>('/api/admin/exercises', {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: EXERCISES_KEY }),
  })
}

export function useUpdateExercise() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ exerciseId, input }: { exerciseId: string; input: UpsertExerciseInput }) =>
      apiFetch<Exercise>(`/api/admin/exercises/${exerciseId}`, {
        method: 'PUT',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    // Plans embed the exercise, so a new video has to reach them too — the coach's plan
    // editor and the member's own plan would otherwise keep showing the old one.
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: EXERCISES_KEY })
      queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY })
      queryClient.invalidateQueries({ queryKey: ['admin-member-plans'] })
      queryClient.invalidateQueries({ queryKey: ['member-training-plan'] })
    },
  })
}

export function useDeleteExercise() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ exerciseId }: { exerciseId: string }) =>
      apiFetch<void>(`/api/admin/exercises/${exerciseId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: EXERCISES_KEY }),
  })
}

/* ---------- plans (coach) ---------- */

export function usePlanTemplates() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: TEMPLATES_KEY,
    queryFn: () =>
      apiFetch<TrainingPlan[]>('/api/admin/training-plan-templates', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useMemberPlans(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: memberPlansKey(memberId),
    queryFn: () =>
      apiFetch<TrainingPlan[]>(`/api/admin/members/${memberId}/training-plans`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token),
  })
}

/**
 * Plan edits all answer with the whole plan, so rather than patching caches by hand every
 * mutation just refetches the two lists a plan can appear in.
 */
function useInvalidatePlans() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY })
    queryClient.invalidateQueries({ queryKey: ['admin-member-plans'] })
  }
}

export function useCreateTemplate() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: (input: UpsertPlanInput) =>
      apiFetch<TrainingPlan>('/api/admin/training-plan-templates', {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: invalidate,
  })
}

export function useCreateMemberPlan(memberId: string) {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ input, templateId }: { input: UpsertPlanInput; templateId?: string }) =>
      apiFetch<TrainingPlan>(
        `/api/admin/members/${memberId}/training-plans${templateId ? `?templateId=${templateId}` : ''}`,
        { method: 'POST', headers: adminHeaders(token!), body: JSON.stringify(input) },
      ),
    onSuccess: invalidate,
  })
}

export function useUpdatePlan() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ planId, input }: { planId: string; input: UpsertPlanInput }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plans/${planId}`, {
        method: 'PUT',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: invalidate,
  })
}

export function useActivatePlan() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ planId }: { planId: string }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plans/${planId}/activation`, {
        method: 'POST',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useDeletePlan() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ planId }: { planId: string }) =>
      apiFetch<void>(`/api/admin/training-plans/${planId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useAddPlanDay() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ planId, name }: { planId: string; name: string }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plans/${planId}/days`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify({ name }),
      }),
    onSuccess: invalidate,
  })
}

export function useDeletePlanDay() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ dayId }: { dayId: string }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plan-days/${dayId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useAddPlanExercise() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ dayId, input }: { dayId: string; input: UpsertPlanExerciseInput }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plan-days/${dayId}/exercises`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: invalidate,
  })
}

export function useDeletePlanExercise() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ planExerciseId }: { planExerciseId: string }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plan-exercises/${planExerciseId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: invalidate,
  })
}

export function useMemberWorkoutLogs(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-member-workout-logs', memberId],
    queryFn: () =>
      apiFetch<WorkoutLog[]>(`/api/admin/members/${memberId}/workout-logs`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token),
  })
}

/* ---------- training (member) ---------- */

export function useMyTrainingPlan() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: ['member-training-plan'],
    // 204 means "no plan assigned yet", which is not an error — but react-query rejects
    // `undefined` as data, so it is normalised to null.
    queryFn: () =>
      apiFetch<TrainingPlan | null>('/api/member/training-plan', {
        headers: memberHeaders(session!.token),
      }).then((plan) => plan ?? null),
    enabled: Boolean(session),
  })
}

export function useMyWorkoutLogs() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: ['member-workout-logs'],
    queryFn: () =>
      apiFetch<WorkoutLog[]>('/api/member/workout-logs', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useSaveWorkoutLog() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: SaveWorkoutLogInput) =>
      apiFetch<WorkoutLog>('/api/member/workout-logs', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(input),
      }),
    onSuccess: () => {
      // The plan carries "last performed", so it has to be refetched alongside the logs.
      queryClient.invalidateQueries({ queryKey: ['member-training-plan'] })
      queryClient.invalidateQueries({ queryKey: ['member-workout-logs'] })
      queryClient.invalidateQueries({ queryKey: ['member-training-stats'] })
    },
  })
}

/** Records, weekly volume and next-session suggestions for the signed-in member. */
export function useMyTrainingStats() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: ['member-training-stats'],
    queryFn: () =>
      apiFetch<TrainingStats>('/api/member/training-stats', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useMemberTrainingStats(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-member-training-stats', memberId],
    queryFn: () =>
      apiFetch<TrainingStats>(`/api/admin/members/${memberId}/training-stats`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}
