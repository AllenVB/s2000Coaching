import { CHECK_IN_SCALES, type CheckIn } from '@/types/checkIn'

/** The four scales and the diet figure of one check-in, as compact chips. */
export function CheckInSummary({ checkIn }: { checkIn: CheckIn }) {
  return (
    <div className="flex flex-wrap gap-1.5">
      {CHECK_IN_SCALES.map((scale) => (
        <span
          key={scale.key}
          className="rounded-full border border-border bg-surface px-2.5 py-0.5 text-[11px] text-text-secondary"
        >
          {scale.label} <strong className="text-text-primary">{checkIn[scale.key]}/5</strong>
        </span>
      ))}
      {checkIn.dietAdherencePercent !== null ? (
        <span className="rounded-full border border-border bg-surface px-2.5 py-0.5 text-[11px] text-text-secondary">
          Beslenmeye uyum <strong className="text-text-primary">%{checkIn.dietAdherencePercent}</strong>
        </span>
      ) : null}
    </div>
  )
}
