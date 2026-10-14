import { useMemo } from 'react'

export interface TrendPoint {
  date: string
  weightKg: number | null
  trendWeightKg: number | null
}

const WIDTH = 720
const HEIGHT = 260
const PAD = { top: 16, right: 16, bottom: 28, left: 44 }
const PLOT_W = WIDTH - PAD.left - PAD.right
const PLOT_H = HEIGHT - PAD.top - PAD.bottom

/**
 * Raw weigh-ins behind their smoothed trend.
 *
 * <p>Two series on one axis, unlike {@link ProgressChart}, and for a specific reason: these
 * are the same measure in the same unit, so the distance between them is meaningful — it is
 * the daily noise the trend exists to remove. Showing the trend alone would hide how much
 * scatter it is drawn through, which is the main thing a reader should weigh when deciding
 * how much to believe it.
 */
export function TrendWeightChart({ points }: { points: TrendPoint[] }) {
  const geometry = useMemo(() => {
    const usable = points.filter((point) => point.weightKg !== null || point.trendWeightKg !== null)
    if (usable.length === 0) {
      return null
    }

    const times = usable.map((point) => Date.parse(point.date))
    const values = usable.flatMap((point) =>
      [point.weightKg, point.trendWeightKg].filter((value): value is number => value !== null),
    )

    const minTime = Math.min(...times)
    const timeSpan = Math.max(...times) - minTime || 1
    const rawMin = Math.min(...values)
    const rawMax = Math.max(...values)
    const pad = (rawMax - rawMin || 1) * 0.12
    const minValue = rawMin - pad
    const valueSpan = rawMax + pad - minValue || 1

    const x = (time: number) =>
      usable.length === 1 ? PAD.left + PLOT_W / 2 : PAD.left + ((time - minTime) / timeSpan) * PLOT_W
    const y = (value: number) => PAD.top + PLOT_H - ((value - minValue) / valueSpan) * PLOT_H

    const raw = usable
      .filter((point) => point.weightKg !== null)
      .map((point) => ({ cx: x(Date.parse(point.date)), cy: y(point.weightKg!), date: point.date }))
    const trend = usable
      .filter((point) => point.trendWeightKg !== null)
      .map((point) => `${x(Date.parse(point.date))},${y(point.trendWeightKg!)}`)
      .join(' ')

    return {
      raw,
      trend,
      ticks: [rawMax + pad, (rawMax + rawMin) / 2, minValue],
      firstDate: usable[0].date,
      lastDate: usable[usable.length - 1].date,
      // A dot per day is a smear once the record runs to hundreds of weigh-ins.
      dotRadius: raw.length > 200 ? 1 : raw.length > 60 ? 1.5 : 2.5,
    }
  }, [points])

  if (!geometry) {
    return null
  }

  return (
    <figure className="m-0">
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} className="w-full" role="img" aria-label="Kilo ve trend">
        {geometry.ticks.map((tick, index) => {
          const ty = PAD.top + (PLOT_H / (geometry.ticks.length - 1)) * index
          return (
            <g key={index}>
              <line
                x1={PAD.left}
                x2={WIDTH - PAD.right}
                y1={ty}
                y2={ty}
                stroke="currentColor"
                className="text-border"
                strokeWidth={1}
              />
              <text
                x={PAD.left - 8}
                y={ty + 4}
                textAnchor="end"
                className="fill-current text-text-muted"
                fontSize={11}
              >
                {tick.toFixed(1)}
              </text>
            </g>
          )
        })}

        <text x={PAD.left} y={HEIGHT - 8} className="fill-current text-text-muted" fontSize={11}>
          {formatDay(geometry.firstDate)}
        </text>
        <text
          x={WIDTH - PAD.right}
          y={HEIGHT - 8}
          textAnchor="end"
          className="fill-current text-text-muted"
          fontSize={11}
        >
          {formatDay(geometry.lastDate)}
        </text>

        {geometry.raw.map((dot) => (
          <circle
            key={dot.date}
            cx={dot.cx}
            cy={dot.cy}
            r={geometry.dotRadius}
            className="fill-current text-text-muted"
            opacity={0.55}
          />
        ))}

        <polyline
          points={geometry.trend}
          fill="none"
          stroke="currentColor"
          className="text-primary"
          strokeWidth={2.5}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>

      <figcaption className="mt-2 flex flex-wrap items-center gap-4 text-[11px] text-text-muted">
        <span className="inline-flex items-center gap-1.5">
          <span className="inline-block h-1.5 w-1.5 rounded-full bg-text-muted" />
          Günlük tartı
        </span>
        <span className="inline-flex items-center gap-1.5">
          <span className="inline-block h-0.5 w-5 rounded bg-primary" />
          Trend (yumuşatılmış)
        </span>
      </figcaption>
    </figure>
  )
}

function formatDay(date: string): string {
  return new Date(date).toLocaleDateString('tr-TR', { day: '2-digit', month: 'short', year: '2-digit' })
}
