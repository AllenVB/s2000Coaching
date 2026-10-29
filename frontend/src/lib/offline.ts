import { readStoredValue, removeStoredValue, writeStoredValue } from '@/lib/storage'
import type { SaveWorkoutLogInput, TrainingPlan } from '@/types/training'

/**
 * What lets the member keep training where there is no signal (a gym basement): the last plan
 * they loaded stays readable, and a finished workout is held on the phone until it can be sent.
 *
 * <p>Everything is keyed by member id, so a phone passed between two people never posts one
 * person's workout under the other's account.
 */

const QUEUE_KEY = 's2000.pendingWorkoutLogs'
const planKey = (memberId: string) => `s2000.cachedPlan.${memberId}`

/** A failed `fetch` (no connection) is a TypeError; an HTTP error answer is an ApiError instead. */
export function isNetworkError(error: unknown): boolean {
  return error instanceof TypeError
}

export interface PendingWorkoutLog {
  memberId: string
  input: SaveWorkoutLogInput
  queuedAt: string
}

export function readPendingLogs(): PendingWorkoutLog[] {
  const raw = readStoredValue(localStorage, QUEUE_KEY)
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw) as PendingWorkoutLog[]
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function writePendingLogs(items: PendingWorkoutLog[]) {
  if (items.length === 0) {
    removeStoredValue(localStorage, QUEUE_KEY)
  } else {
    writeStoredValue(localStorage, QUEUE_KEY, JSON.stringify(items))
  }
}

export function pendingLogCount(memberId: string): number {
  return readPendingLogs().filter((item) => item.memberId === memberId).length
}

/**
 * Holds a finished workout. The server replaces a day's log for the same date, so saving the
 * same day again offline must replace the queued copy rather than stack a second one.
 */
export function enqueueWorkoutLog(memberId: string, input: SaveWorkoutLogInput) {
  const rest = readPendingLogs().filter(
    (item) =>
      !(item.memberId === memberId && item.input.dayId === input.dayId && item.input.performedOn === input.performedOn),
  )
  writePendingLogs([...rest, { memberId, input, queuedAt: new Date().toISOString() }])
}

/**
 * Sends the member's queued workouts. Stops at the first failure so order is kept and nothing
 * is dropped: a network error leaves everything queued, a rejected one (say the plan day was
 * deleted meanwhile) is discarded, because retrying a request the server refuses would block
 * every workout behind it forever.
 *
 * @return how many were delivered
 */
export async function flushPendingLogs(
  memberId: string,
  send: (input: SaveWorkoutLogInput) => Promise<unknown>,
): Promise<number> {
  let delivered = 0
  for (const item of readPendingLogs().filter((entry) => entry.memberId === memberId)) {
    try {
      await send(item.input)
    } catch (error) {
      if (isNetworkError(error)) {
        return delivered
      }
      // Refused by the server: not coming back, so it must not block the rest.
    }
    writePendingLogs(readPendingLogs().filter((entry) => entry !== item && !sameLog(entry, item)))
    delivered += 1
  }
  return delivered
}

function sameLog(a: PendingWorkoutLog, b: PendingWorkoutLog): boolean {
  return a.memberId === b.memberId && a.queuedAt === b.queuedAt && a.input.dayId === b.input.dayId
}

export function cachePlan(memberId: string, plan: TrainingPlan | null) {
  if (plan) {
    writeStoredValue(localStorage, planKey(memberId), JSON.stringify(plan))
  }
}

export function readCachedPlan(memberId: string): TrainingPlan | null {
  const raw = readStoredValue(localStorage, planKey(memberId))
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as TrainingPlan
  } catch {
    return null
  }
}

/**
 * Called on sign-out: the cached plan should not stay readable on a shared phone. Unsent
 * workouts are deliberately kept — they belong to that member and are delivered the next time
 * they sign in, instead of being lost with the session.
 */
export function clearCachedPlan(memberId: string) {
  removeStoredValue(localStorage, planKey(memberId))
}
