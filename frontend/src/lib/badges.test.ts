import { describe, expect, it } from 'vitest'

import { computeBadges, goalProgress, longestRun } from './badges'

const empty = { workoutDates: [], intakeDates: [], steps: [], periods: [], goalReached: false }
const earned = (input: Parameters<typeof computeBadges>[0], id: string) => computeBadges(input).find((b) => b.id === id)?.earned

describe('longestRun', () => {
  it('finds the longest run of consecutive numbers, ignoring order and repeats', () => {
    expect(longestRun([5, 3, 4, 4, 9, 10])).toBe(3)
    expect(longestRun([])).toBe(0)
    expect(longestRun([7])).toBe(1)
  })
})

describe('goalProgress', () => {
  it('measures a loss from the start towards the goal', () => {
    expect(goalProgress(90, 85, 80)).toBe(50)
    expect(goalProgress(90, 80, 80)).toBe(100)
    expect(goalProgress(90, 95, 80)).toBe(0)
  })
  it('measures a gain too', () => {
    expect(goalProgress(70, 75, 80)).toBe(50)
  })
  it('has no progress without a goal or when the start already is the goal', () => {
    expect(goalProgress(90, 85, null)).toBeNull()
    expect(goalProgress(80, 80, 80)).toBeNull()
  })
})

describe('badges', () => {
  it('earns the workout-count badges from the number of logs', () => {
    const dates = Array.from({ length: 10 }, (_, i) => `2026-10-${String(i + 1).padStart(2, '0')}`)
    expect(earned({ ...empty, workoutDates: dates }, 'workouts-10')).toBe(true)
    expect(earned({ ...empty, workoutDates: dates.slice(0, 9) }, 'workouts-10')).toBe(false)
  })

  it('needs a workout in each of four consecutive Monday-to-Sunday weeks for the streak', () => {
    // Mondays 5, 12, 19, 26 October 2026 are four consecutive weeks.
    const four = ['2026-10-05', '2026-10-14', '2026-10-25', '2026-10-26']
    expect(earned({ ...empty, workoutDates: four }, 'week-streak-4')).toBe(true)
    // Sunday the 11th belongs to the week of the 5th, so skipping the week of the 12th breaks the run.
    const broken = ['2026-10-05', '2026-10-11', '2026-10-25', '2026-10-26']
    expect(earned({ ...empty, workoutDates: broken }, 'week-streak-4')).toBe(false)
  })

  it('counts seven food-log days in a row, not seven days in total', () => {
    const run = Array.from({ length: 7 }, (_, i) => `2026-10-${String(i + 10).padStart(2, '0')}`)
    expect(earned({ ...empty, intakeDates: run }, 'food-7')).toBe(true)
    expect(earned({ ...empty, intakeDates: [...run.slice(0, 3), ...run.slice(4)] }, 'food-7')).toBe(false)
  })

  it('earns the steps badge at 10 000 and the monthly one at three consecutive months', () => {
    expect(earned({ ...empty, steps: [{ date: '2026-10-01', steps: 10000 }] }, 'steps-10k')).toBe(true)
    expect(earned({ ...empty, steps: [{ date: '2026-10-01', steps: 9999 }] }, 'steps-10k')).toBe(false)
    expect(earned({ ...empty, periods: ['2026-11', '2026-12', '2027-01'] }, 'months-3')).toBe(true)
    expect(earned({ ...empty, periods: ['2026-09', '2026-10', '2026-12'] }, 'months-3')).toBe(false)
  })
})
