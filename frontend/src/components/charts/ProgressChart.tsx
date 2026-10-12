import { useMemo, useState } from 'react'

export interface ProgressPoint {
  /** ISO date (yyyy-mm-dd) — the x position is the real elapsed time, not the index. */
  date: string
  value: number
}

const WIDTH = 720
const HEIGHT = 240
const PAD = { top: 16, right: 16, bottom: 28, left: 44 }
const PLOT_W = WIDTH - PAD.left - PAD.right
const PLOT_H = HEIGHT - PAD.top - PAD.bottom

/**
 * Single-series time chart for one measure at a time (weight, waist, …). Deliberately one
 * series and one axis: a second measure on a second scale would make the two lines'
 * crossings meaningless, so switching measures swaps the series instead of adding one.
 */
export function ProgressChart({
  points,
  unit,
  label,
}: {
  points: ProgressPoint[]
  unit: string
  label: string
}) {
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)

  const geometry = useMemo(() => {
    if (points.length === 0) {
      return null
    }
    const times = points.map((point) => Date.parse(point.date))
    const values = points.map((point) => point.value)

    const minTime = Math.min(...times)
    const maxTime = Math.max(...times)
    const timeSpan = maxTime - minTime || 1

    const rawMin = Math.min(...values)
    const rawMax = Math.max(...values)
    // Pad the value axis so a flat series does not collapse onto the baseline.
    const pad = (rawMax - rawMin || Math.max(rawMax * 0.04, 1)) * 0.15
    const minValue = rawMin - pad
    const maxValue = rawMax + pad
    const valueSpan = maxValue - minValue || 1

    const x = (time: number) =>
      points.length === 1 ? PAD.left + PLOT_W / 2 : PAD.left + ((time - minTime) / timeSpan) * PLOT_W
    const y = (value: number) => PAD.top + PLOT_H - ((value - minValue) / valueSpan) * PLOT_H

    const coords = points.map((point, index) => ({
      ...point,
      index,
      cx: x(times[index]),
      cy: y(point.value),
    }))

    const ticks = [maxValue, (maxValue + minValue) / 2, minValue]

    return { coords, ticks, path: coords.map((c) => `${c.cx},${c.cy}`).join(' ') }
  }, [points])

  if (!geometry) {
    return null
  }

  const { coords, ticks, path } = geometry
  const active = hoverIndex !== null ? coords[hoverIndex] : null
  const last = coords[coords.length - 1]
  const showMarkers = coords.length <= 24

  const handleMove = (event: React.MouseEvent<SVGSVGElement>) => {
    const rect = event.currentTarget.getBoundingClientRect()
    const ratio = (event.clientX - rect.left) / rect.width
    const svgX = ratio * WIDTH
    let nearest = 0
    coords.forEach((coord, index) => {
      if (Math.abs(coord.cx - svgX) < Math.abs(coords[nearest].cx - svgX)) {
        nearest = index
      }
    })
    setHoverIndex(nearest)
  }

  return (
    <figure className="relative m-0">
      <svg
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        className="w-full"
        role="img"
        aria-label={`${label} değişimi`}
        onMouseMove={handleMove}
        onMouseLeave={() => setHoverIndex(null)}
      >
        {/* Recessive grid: present enough to read a value off, quiet enough to ignore. */}
        {ticks.map((tick, index) => {
          const ty = PAD.top + (PLOT_H / (ticks.length - 1)) * index
          return (
            <g key={tick}>
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
                {formatTick(tick)}
              </text>
            </g>
          )
        })}

        <text x={PAD.left} y={HEIGHT - 8} className="fill-current text-text-muted" fontSize={11}>
          {formatDay(coords[0].date)}
        </text>
        {coords.length > 1 ? (
          <text
            x={WIDTH - PAD.right}
            y={HEIGHT - 8}
            textAnchor="end"
            className="fill-current text-text-muted"
            fontSize={11}
          >
            {formatDay(last.date)}
          </text>
        ) : null}

        {coords.length > 1 ? (
          <polyline
            points={path}
            fill="none"
            stroke="currentColor"
            className="text-primary"
            strokeWidth={2}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        ) : null}

        {showMarkers
          ? coords.map((coord) => (
              <circle
                key={coord.date}
                cx={coord.cx}
                cy={coord.cy}
                r={4}
                className="fill-current text-primary"
                stroke="var(--color-surface)"
                strokeWidth={2}
              />
            ))
          : null}

        {active ? (
          <>
            <line
              x1={active.cx}
              x2={active.cx}
              y1={PAD.top}
              y2={PAD.top + PLOT_H}
              stroke="currentColor"
              className="text-border-strong"
              strokeWidth={1}
            />
            <circle
              cx={active.cx}
              cy={active.cy}
              r={6}
              className="fill-current text-primary"
              stroke="var(--color-surface)"
              strokeWidth={2}
            />
          </>
        ) : null}

        {/* Only the latest value is labelled directly; a number on every point is noise. */}
        <text
          x={Math.min(last.cx + 8, WIDTH - PAD.right)}
          y={Math.max(last.cy - 10, PAD.top + 10)}
          textAnchor={last.cx > WIDTH - 90 ? 'end' : 'start'}
          className="fill-current text-text-primary"
          fontSize={12}
          fontWeight={600}
        >
          {formatTick(last.value)} {unit}
        </text>
      </svg>

      {active ? (
        <div
          className="pointer-events-none absolute -translate-x-1/2 -translate-y-full rounded-lg border border-border bg-surface-high px-2.5 py-1.5 text-[11px] shadow-lg"
          style={{ left: `${(active.cx / WIDTH) * 100}%`, top: `${(active.cy / HEIGHT) * 100}%` }}
        >
          <span className="block font-semibold text-text-primary">
            {formatTick(active.value)} {unit}
          </span>
          <span className="block text-text-muted">{formatDay(active.date)}</span>
        </div>
      ) : null}

      <figcaption className="sr-only">
        {label} değişimi: {coords.map((c) => `${formatDay(c.date)} ${c.value} ${unit}`).join(', ')}
      </figcaption>
    </figure>
  )
}

function formatTick(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1)
}

function formatDay(date: string): string {
  return new Date(date).toLocaleDateString('tr-TR', { day: '2-digit', month: 'short' })
}
