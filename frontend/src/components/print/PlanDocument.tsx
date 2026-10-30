import { formatDate } from '@/lib/date'
import { MAX_SETS, muscleGroupLabel, type PlanExercise, type TrainingPlan } from '@/types/training'
import type { Measurement } from '@/types/measurement'
import { dayTypeLabel, hasTargets, mealsOfDay, splitBlocks, targetsOf, type Meal, type NutritionDayType, type NutritionPlan } from '@/types/nutrition'

/** The weight-tracking grid is ten sessions wide, like the strip in the member's workout page. */
const GRID_SESSIONS = Array.from({ length: 10 }, (_unused, index) => index + 1)

export interface PlanDocumentMember {
  fullName: string
  heightCm: number
  /** Weight at sign-up; the latest measurement is shown beside it when there is one. */
  weightKg: number
}

/**
 * The plan as a sheet of paper: what the coach used to lay out by hand in a document. It is
 * drawn in plain black on white on purpose — on screen it reads as a preview of the page, and
 * on a printer it needs no ink-saving workarounds — and it only prints what the system actually
 * has, so a measurement nobody took is left out rather than shown as zero.
 */
export function PlanDocument({
  member,
  measurements,
  nutrition,
  training,
  issuedOn,
}: {
  member: PlanDocumentMember
  measurements: Measurement[]
  nutrition: NutritionPlan | null
  training: TrainingPlan | null
  issuedOn: string
}) {
  return (
    <article className="plan-doc mx-auto w-full max-w-[1120px] bg-white p-6 text-[11px] leading-snug text-neutral-900 shadow-xl print:max-w-none print:p-0 print:shadow-none">
      <header className="mb-4 flex items-end justify-between gap-4 border-b-2 border-neutral-900 pb-2">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.25em] text-orange-700">S2000 Coaching</p>
          <h1 className="mt-0.5 text-2xl font-extrabold uppercase tracking-tight">{member.fullName}</h1>
        </div>
        <p className="text-right text-[10px] text-neutral-600">
          Hazırlanma tarihi
          <br />
          <strong className="text-xs text-neutral-900">{formatDate(issuedOn)}</strong>
        </p>
      </header>

      {nutrition ? (
        <NutritionSection member={member} measurements={measurements} plan={nutrition} dayType="TRAINING" />
      ) : null}
      {/* The rest-day half goes on its own page, and only when the coach has written something for it. */}
      {nutrition && (mealsOfDay(nutrition, 'REST').length > 0 || hasTargets(targetsOf(nutrition, 'REST'))) ? (
        <div className="break-before-page print:break-before-page">
          <NutritionSection member={member} measurements={measurements} plan={nutrition} dayType="REST" />
        </div>
      ) : null}

      {training ? (
        <TrainingSection plan={training} breakBefore={nutrition !== null} member={member} measurements={measurements} showMember={nutrition === null} />
      ) : null}

      {!nutrition && !training ? (
        <p className="py-10 text-center text-sm text-neutral-600">
          Bu üyenin aktif bir antrenman ya da beslenme planı yok; yazdırılacak bir şey bulunamadı.
        </p>
      ) : null}
    </article>
  )
}

/* ---------- member card ---------- */

/** The latest figure for each measure, so one missed tape does not blank the whole card. */
function latestFigures(measurements: Measurement[]) {
  const newestFirst = [...measurements].sort((a, b) => b.measuredOn.localeCompare(a.measuredOn))
  const pick = (read: (m: Measurement) => number | null) => newestFirst.find((m) => read(m) !== null)
  const fields: { label: string; unit: string; read: (m: Measurement) => number | null }[] = [
    { label: 'Bel', unit: 'cm', read: (m) => m.waistCm },
    { label: 'Göğüs', unit: 'cm', read: (m) => m.chestCm },
    { label: 'Kalça', unit: 'cm', read: (m) => m.hipCm },
    { label: 'Kol', unit: 'cm', read: (m) => m.armCm },
    { label: 'Bacak', unit: 'cm', read: (m) => m.thighCm },
  ]
  return {
    latestWeight: newestFirst[0] ?? null,
    circumferences: fields.flatMap((field) => {
      const row = pick(field.read)
      return row ? [{ label: field.label, unit: field.unit, value: field.read(row) as number, on: row.measuredOn }] : []
    }),
  }
}

/** Body-mass index from height and the latest weight; a plain formula, not an estimate of anything else. */
function bodyMassIndex(heightCm: number, weightKg: number): string {
  const meters = heightCm / 100
  return (weightKg / (meters * meters)).toFixed(1)
}

function MemberCard({ member, measurements }: { member: PlanDocumentMember; measurements: Measurement[] }) {
  const { latestWeight, circumferences } = latestFigures(measurements)
  const currentWeight = latestWeight?.weightKg ?? member.weightKg

  return (
    <section className="break-inside-avoid rounded border border-neutral-400">
      <h2 className="border-b border-neutral-400 bg-neutral-100 px-2 py-1 text-[10px] font-bold uppercase tracking-wider">
        Üye bilgileri
      </h2>
      <dl className="divide-y divide-neutral-200">
        <Row label="Boy" value={`${member.heightCm} cm`} />
        <Row label="Başlangıç kilosu" value={`${member.weightKg} kg`} />
        {latestWeight ? (
          <Row label={`Güncel kilo (${formatDate(latestWeight.measuredOn)})`} value={`${latestWeight.weightKg} kg`} />
        ) : null}
        <Row label="VKİ (boy ve güncel kiloya göre)" value={bodyMassIndex(member.heightCm, currentWeight)} />
        {circumferences.map((figure) => (
          <Row key={figure.label} label={figure.label} value={`${figure.value} ${figure.unit}`} />
        ))}
      </dl>
    </section>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-2 px-2 py-1">
      <dt className="text-neutral-600">{label}</dt>
      <dd className="font-semibold">{value}</dd>
    </div>
  )
}

/* ---------- nutrition ---------- */

function NutritionSection({
  member,
  measurements,
  plan,
  dayType,
}: {
  member: PlanDocumentMember
  measurements: Measurement[]
  plan: NutritionPlan
  dayType: NutritionDayType
}) {
  const { meals, supplements } = splitBlocks(plan, dayType)
  const dayTargets = targetsOf(plan, dayType)
  const targets = [
    dayTargets.calories !== null ? { label: 'Kalori', value: `${dayTargets.calories} kcal` } : null,
    dayTargets.proteinG !== null ? { label: 'Protein', value: `${dayTargets.proteinG} g` } : null,
    dayTargets.carbsG !== null ? { label: 'Karbonhidrat', value: `${dayTargets.carbsG} g` } : null,
    dayTargets.fatG !== null ? { label: 'Yağ', value: `${dayTargets.fatG} g` } : null,
  ].filter((target): target is { label: string; value: string } => target !== null)

  return (
    <section>
      <SectionTitle>
        Beslenme programı · {plan.name} · {dayTypeLabel(dayType)}
      </SectionTitle>
      <div className="grid grid-cols-[180px_1fr_1fr_1fr] items-start gap-3">
        <div className="space-y-3">
          <MemberCard member={member} measurements={measurements} />
          {targets.length > 0 || plan.waterLiters !== null ? (
            <section className="break-inside-avoid rounded border border-neutral-400">
              <h2 className="border-b border-neutral-400 bg-neutral-100 px-2 py-1 text-[10px] font-bold uppercase tracking-wider">
                Günlük hedefler
              </h2>
              <dl className="divide-y divide-neutral-200">
                {targets.map((target) => (
                  <Row key={target.label} label={target.label} value={target.value} />
                ))}
                {plan.waterLiters !== null ? <Row label="Su" value={`${plan.waterLiters} litre`} /> : null}
              </dl>
            </section>
          ) : null}
        </div>

        {/* Meals run down two columns so the whole nutrition plan fits one landscape page. */}
        {balancedHalves(meals).map((half, index) => (
          <div key={index} className="space-y-3">
            {half.map((meal) => (
              <BlockTable key={meal.id} block={meal} />
            ))}
            {index === 0 && meals.length === 0 ? <p className="text-neutral-500">Öğün yok.</p> : null}
          </div>
        ))}

        <div className="space-y-3">
          {supplements.length > 0 ? <SubTitle>Supplement planı</SubTitle> : null}
          {supplements.map((block) => (
            <BlockTable key={block.id} block={block} />
          ))}
          {plan.note ? (
            <section className="break-inside-avoid rounded border border-neutral-400 p-2">
              <h2 className="mb-1 text-[10px] font-bold uppercase tracking-wider">Notlar</h2>
              <p className="whitespace-pre-line">{plan.note}</p>
            </section>
          ) : null}
        </div>
      </div>
    </section>
  )
}

/**
 * Splits the meals, in order, into two columns of about the same height (a block's height is
 * its rows plus its title), so neither column runs onto a second page while the other is short.
 */
function balancedHalves(meals: Meal[]): [Meal[], Meal[]] {
  const height = (meal: Meal) => meal.items.length + 2
  const total = meals.reduce((sum, meal) => sum + height(meal), 0)
  let running = 0
  let best = 0
  let bestGap = Number.POSITIVE_INFINITY
  meals.forEach((meal, index) => {
    running += height(meal)
    const gap = Math.abs(total - 2 * running)
    if (index < meals.length - 1 && gap < bestGap) {
      best = index + 1
      bestGap = gap
    }
  })
  return meals.length < 2 ? [meals, []] : [meals.slice(0, best), meals.slice(best)]
}

function BlockTable({ block }: { block: Meal }) {
  return (
    <table className="w-full break-inside-avoid border-collapse rounded border border-neutral-400 text-left">
      <thead>
        <tr>
          <th colSpan={2} className="border-b border-neutral-400 bg-neutral-100 px-2 py-1 text-[10px] font-bold uppercase tracking-wider">
            {block.name}
            {block.timeHint ? <span className="ml-2 font-normal normal-case text-neutral-600">{block.timeHint}</span> : null}
          </th>
        </tr>
      </thead>
      <tbody>
        {block.items.map((item) => (
          <tr key={item.id} className="border-b border-neutral-200 last:border-b-0">
            <td className="px-2 py-1">
              {item.name}
              {item.note ? <span className="block text-[10px] text-neutral-600">{item.note}</span> : null}
            </td>
            {/* An amount the coach has not written leaves a blank to fill in by hand. */}
            <td className="w-[38%] border-l border-neutral-200 px-2 py-1 text-right font-semibold">
              {item.amount ?? ' '}
            </td>
          </tr>
        ))}
        {block.items.length === 0 ? (
          <tr>
            <td colSpan={2} className="px-2 py-1 text-neutral-500">
              —
            </td>
          </tr>
        ) : null}
      </tbody>
    </table>
  )
}

/* ---------- training ---------- */

function TrainingSection({
  plan,
  breakBefore,
  member,
  measurements,
  showMember,
}: {
  plan: TrainingPlan
  breakBefore: boolean
  member: PlanDocumentMember
  measurements: Measurement[]
  showMember: boolean
}) {
  // An empty day is a placeholder in the editor, not something to hand to a member.
  const days = plan.days.filter((day) => day.exercises.length > 0)

  return (
    <section className={breakBefore ? 'mt-6 break-before-page print:mt-0' : ''}>
      <SectionTitle>Antrenman programı · {plan.name}</SectionTitle>
      {plan.note ? <p className="mb-3 whitespace-pre-line rounded border border-neutral-400 p-2">{plan.note}</p> : null}
      <div className={showMember ? 'grid grid-cols-[190px_1fr] items-start gap-4' : ''}>
        {showMember ? <MemberCard member={member} measurements={measurements} /> : null}
        <div className="space-y-4">
          {days.map((day) => (
            <DayBlock key={day.id} name={day.name} note={day.note} exercises={day.exercises} />
          ))}
          {days.length === 0 ? <p className="text-neutral-500">Planda hareket yok.</p> : null}
        </div>
      </div>
    </section>
  )
}

/** What the set cells say: each set as written, or the older single "reps" repeated per set. */
function setCells(planExercise: PlanExercise): { cells: string[]; overflow: string | null } {
  const perSet = planExercise.setReps ?? []
  if (perSet.length > 0) {
    return { cells: perSet, overflow: null }
  }
  if (planExercise.setCount > MAX_SETS) {
    return { cells: [], overflow: `${planExercise.setCount} set × ${planExercise.reps}` }
  }
  return { cells: Array.from({ length: planExercise.setCount }, () => planExercise.reps), overflow: null }
}

/** Set columns on the sheet: at least four, so there is room to write, and as many as the longest exercise has. */
function setColumnsOf(exercises: PlanExercise[]): number[] {
  const longest = Math.max(0, ...exercises.map((planExercise) => setCells(planExercise).cells.length))
  return Array.from({ length: Math.min(MAX_SETS, Math.max(4, longest)) }, (_unused, index) => index + 1)
}

function DayBlock({ name, note, exercises }: { name: string; note: string | null; exercises: PlanExercise[] }) {
  const SET_NUMBERS = setColumnsOf(exercises)
  return (
    <div className="break-inside-avoid">
      <table className="w-full border-collapse border border-neutral-400 text-left">
        <thead>
          <tr className="bg-neutral-100 text-[10px] uppercase tracking-wider">
            <th className="border-b border-neutral-400 px-2 py-1 text-xs font-bold normal-case tracking-normal" colSpan={2}>
              {name}
            </th>
            {SET_NUMBERS.map((number) => (
              <th key={number} className="w-12 border-b border-l border-neutral-300 px-1 py-1 text-center">
                Set {number}
              </th>
            ))}
            <th className="w-14 border-b border-l border-neutral-300 px-1 py-1 text-center">Hedef kg</th>
            <th className="w-16 border-b border-l border-neutral-300 px-1 py-1 text-center">Dinlenme</th>
          </tr>
        </thead>
        <tbody>
          {exercises.map((planExercise) => {
            const { cells, overflow } = setCells(planExercise)
            return (
              <tr key={planExercise.id} className="border-b border-neutral-200 last:border-b-0">
                <td className="px-2 py-1 font-semibold" colSpan={2}>
                  {planExercise.exercise.name}
                  <span className="ml-1.5 text-[10px] font-normal text-neutral-500">
                    {muscleGroupLabel(planExercise.exercise.muscleGroup)}
                  </span>
                  {planExercise.note ? <span className="block text-[10px] font-normal text-neutral-600">{planExercise.note}</span> : null}
                </td>
                {overflow ? (
                  <td colSpan={SET_NUMBERS.length} className="border-l border-neutral-200 px-1 py-1 text-center">
                    {overflow}
                  </td>
                ) : (
                  SET_NUMBERS.map((number) => (
                    <td key={number} className="border-l border-neutral-200 px-1 py-1 text-center">
                      {cells[number - 1] ?? ''}
                    </td>
                  ))
                )}
                <td className="border-l border-neutral-200 px-1 py-1 text-center">{planExercise.targetWeightKg ?? ''}</td>
                <td className="border-l border-neutral-200 px-1 py-1 text-center">
                  {planExercise.restSeconds !== null ? `${planExercise.restSeconds} sn` : ''}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
      {note ? <p className="mt-1 whitespace-pre-line text-neutral-700">{note}</p> : null}

      {/* Ten empty boxes per exercise: the weight lifted in each of the next sessions, to fill in by hand. */}
      <table className="mt-2 w-full border-collapse border border-neutral-400 text-center">
        <thead>
          <tr className="bg-neutral-100 text-[9px] uppercase tracking-wide">
            <th className="w-[26%] border-b border-neutral-400 px-2 py-1 text-left">Ağırlık takibi (kg)</th>
            {GRID_SESSIONS.map((session) => (
              <th key={session} className="border-b border-l border-neutral-300 px-0.5 py-1">
                Antr. {session}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {exercises.map((planExercise) => (
            <tr key={planExercise.id} className="border-b border-neutral-200 last:border-b-0">
              <td className="px-2 py-1.5 text-left">{planExercise.exercise.name}</td>
              {GRID_SESSIONS.map((session) => (
                <td key={session} className="border-l border-neutral-200">
                  &nbsp;
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/* ---------- small pieces ---------- */

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="mb-2 border-l-4 border-orange-600 pl-2 text-sm font-extrabold uppercase tracking-wide">{children}</h2>
  )
}

function SubTitle({ children }: { children: React.ReactNode }) {
  return <h3 className="text-[10px] font-bold uppercase tracking-wider text-neutral-600">{children}</h3>
}
