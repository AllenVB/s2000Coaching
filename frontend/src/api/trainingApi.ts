import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch, apiUpload } from '@/lib/api'
import { cachePlan, enqueueWorkoutLog, flushPendingLogs, isNetworkError, readCachedPlan } from '@/lib/offline'
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

/** Replaces the exercise's demonstration image (JPEG or PNG; the server re-encodes it). */
export function useSetExerciseImage() {
  const { token } = useAdminAuth()
  const refresh = useRefreshExercises()

  return useMutation({
    mutationFn: ({ exerciseId, file }: { exerciseId: string; file: File }) => {
      const body = new FormData()
      body.append('file', file)
      return apiUpload<Exercise>(`/api/admin/exercises/${exerciseId}/image`, body, adminHeaders(token!), 'PUT')
    },
    onSuccess: refresh,
  })
}

export function useRemoveExerciseImage() {
  const { token } = useAdminAuth()
  const refresh = useRefreshExercises()

  return useMutation({
    mutationFn: ({ exerciseId }: { exerciseId: string }) =>
      apiFetch<Exercise>(`/api/admin/exercises/${exerciseId}/image`, { method: 'DELETE', headers: adminHeaders(token!) }),
    onSuccess: refresh,
  })
}

/** Plans embed the exercise, so a changed image has to reach the lists they appear in too. */
function useRefreshExercises() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: EXERCISES_KEY })
    queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY })
    queryClient.invalidateQueries({ queryKey: ['admin-member-plans'] })
    queryClient.invalidateQueries({ queryKey: ['member-training-plan'] })
  }
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
    mutationFn: ({ input, blank }: { input: UpsertPlanInput; blank?: boolean }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plan-templates${blank ? '?blank=true' : ''}`, {
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
    // Without a template the server starts the plan with empty Gün 1–5; `blank` opts out.
    mutationFn: ({
      input,
      templateId,
      blank,
    }: {
      input: UpsertPlanInput
      templateId?: string
      blank?: boolean
    }) => {
      const query = templateId ? `?templateId=${templateId}` : blank ? '?blank=true' : ''
      return apiFetch<TrainingPlan>(`/api/admin/members/${memberId}/training-plans${query}`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      })
    },
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

/** Replaces the whole prescription, so a cell edit has to send the row's other fields back. */
export function useUpdatePlanExercise() {
  const { token } = useAdminAuth()
  const invalidate = useInvalidatePlans()

  return useMutation({
    mutationFn: ({ planExerciseId, input }: { planExerciseId: string; input: UpsertPlanExerciseInput }) =>
      apiFetch<TrainingPlan>(`/api/admin/training-plan-exercises/${planExerciseId}`, {
        method: 'PUT',
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
    // react-query would pause this while the browser reports offline, never reaching the
    // fallback below; 'always' lets the request run and fail, which is how offline is detected.
    networkMode: 'always',
    queryFn: async () => {
      try {
        const plan = (await apiFetch<TrainingPlan | null>('/api/member/training-plan', {
          headers: memberHeaders(session!.token),
        })) ?? null
        cachePlan(session!.member.id, plan)
        return plan
      } catch (error) {
        // No connection: the last plan this phone loaded is better than a blank screen.
        const cached = isNetworkError(error) ? readCachedPlan(session!.member.id) : null
        if (cached) {
          return cached
        }
        throw error
      }
    },
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
    // Must run while offline: the failure is what sends the workout to the on-phone queue.
    networkMode: 'always',
    mutationFn: async (input: SaveWorkoutLogInput): Promise<WorkoutLog | { queued: true }> => {
      try {
        return await apiFetch<WorkoutLog>('/api/member/workout-logs', {
          method: 'POST',
          headers: memberHeaders(session!.token),
          body: JSON.stringify(input),
        })
      } catch (error) {
        if (isNetworkError(error)) {
          enqueueWorkoutLog(session!.member.id, input)
          return { queued: true }
        }
        throw error
      }
    },
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

/** Sends workouts saved while offline as soon as there is a connection again. */
export function useFlushPendingWorkouts() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!session) {
      return
    }
    const memberId = session.member.id
    const token = session.token
    let running = false

    const flush = async () => {
      if (running) {
        return
      }
      running = true
      try {
        const delivered = await flushPendingLogs(memberId, (input) =>
          apiFetch<WorkoutLog>('/api/member/workout-logs', {
            method: 'POST',
            headers: memberHeaders(token),
            body: JSON.stringify(input),
          }),
        )
        if (delivered > 0) {
          queryClient.invalidateQueries({ queryKey: ['member-training-plan'] })
          queryClient.invalidateQueries({ queryKey: ['member-workout-logs'] })
          queryClient.invalidateQueries({ queryKey: ['member-training-stats'] })
        }
      } finally {
        running = false
      }
    }

    void flush()
    window.addEventListener('online', flush)
    return () => window.removeEventListener('online', flush)
  }, [session, queryClient])
}
