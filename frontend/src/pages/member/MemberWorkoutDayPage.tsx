import { Link, useParams } from 'react-router-dom'

import { useMyTrainingPlan } from '@/api/trainingApi'
import { MemberExerciseImage } from '@/components/training/ExerciseImage'
import { ExerciseVideo } from '@/components/training/ExerciseVideo'
import { RestTimer } from '@/components/training/RestTimer'
import { ArrowLeftIcon } from '@/components/ui/icons'

/** One day of the assigned programme, to read: the exercises with their image, video, sets and notes. */
export function MemberWorkoutDayPage() {
  const { dayId } = useParams<{ dayId: string }>()
  const { data: plan, isLoading } = useMyTrainingPlan()
  const day = plan?.days.find((candidate) => candidate.id === dayId)

  if (isLoading) {
    return <div className="h-64 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (!day) {
    return (
      <>
        <BackLink />
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Bu antrenman günü programınızda bulunamadı.
        </p>
      </>
    )
  }

  return (
    <>
      <BackLink />
      <div className="mb-6">
        <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary sm:text-3xl">
          {day.name}
        </h1>
        <p className="mt-2 text-sm text-text-secondary">
          Programı koçunuz hazırladı. Yaptığınız tekrarları{' '}
          <Link to="/uye/gecmis" className="font-semibold text-primary hover:underline">
            Geçmiş
          </Link>{' '}
          sayfasından girersiniz.
        </p>
      </div>

      <div className="space-y-4">
        {day.exercises.map((planExercise) => (
          <section key={planExercise.id} className="rounded-2xl border border-border bg-surface p-5">
            <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
              <h2 className="font-semibold text-text-primary">{planExercise.exercise.name}</h2>
              {planExercise.targetWeightKg !== null ? (
                <span className="text-xs text-text-muted">Hedef: {planExercise.targetWeightKg} kg</span>
              ) : null}
            </div>

            <MemberExerciseImage exercise={planExercise.exercise} />

            {planExercise.note ? (
              <p className="mb-3 text-xs leading-relaxed text-text-secondary">{planExercise.note}</p>
            ) : null}

            <div className="mb-3">
              <ExerciseVideo exercise={planExercise.exercise} />
            </div>

            <ul className="space-y-1.5">
              {Array.from({ length: planExercise.setCount }, (_unused, index) => (
                <li key={index} className="flex items-center gap-3 text-sm">
                  <span className="w-12 shrink-0 text-xs font-semibold uppercase tracking-wider text-text-muted">
                    {index + 1}. set
                  </span>
                  <span className="text-text-primary">{repsText(planExercise.setReps?.[index] || planExercise.reps)}</span>
                </li>
              ))}
            </ul>

            {planExercise.restSeconds ? (
              <div className="mt-3">
                <RestTimer seconds={planExercise.restSeconds} />
              </div>
            ) : null}
          </section>
        ))}
        {day.exercises.length === 0 ? (
          <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
            Bu güne henüz egzersiz eklenmedi.
          </p>
        ) : null}
      </div>
    </>
  )
}

/** "12" and "8-10" are repetitions; "60 sn" or "AMRAP" already say what they are. */
function repsText(value: string): string {
  return /^[\d\s\-–]+$/.test(value) ? `${value} tekrar` : value
}

function BackLink() {
  return (
    <Link
      to="/uye/programlarim"
      className="mb-4 inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-text-muted transition-colors hover:text-text-primary"
    >
      <ArrowLeftIcon className="h-3.5 w-3.5" />
      Programlarım
    </Link>
  )
}
