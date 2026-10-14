import { useMemo, useState } from 'react'

export interface ScatterPoint {
  x: number
  y: number
  label: string
}

const WIDTH = 720
const HEIGHT = 280
const PAD = { top: 16, right: 20, bottom: 40, left: 52 }
const PLOT_W = WIDTH - PAD.left - PAD.right
const PLOT_H = HEIGHT - PAD.top - PAD.bottom

/**
 * One dot per week: calories against the weight change that followed.
 *
 * <p>No trend line is drawn through the cloud. A line would read as a prediction, and with
 * the dozen-or-so weeks this kind of record holds, its slope moves a long way when any one
 * week is removed. The dots say what happened; the correlation figure printed beside the
 * chart, with its sample size, says how much of a pattern that is.
 */
export function ScatterChart({
  points,
  xLabel,
  yLabel,
  xUnit,
  yUnit,
}: {
  points: ScatterPoint[]
  xLabel: string
  yLabel: string
  xUnit: string
  yUnit: string
}) {
  const [hover, setHover] = useState<number | null>(null)

  const geometry = useMemo(() => {
    if (points.length === 0) {
      return null
    }
    const xs = points.map((point) => point.x)
    const ys = points.map((point) => point.y)

    const xMin = Math.min(...xs)
    const xMax = Math.max(...xs)
    const xPad = (xMax - xMin || 1) * 0.08
    const xLo = xMin - xPad
    const xSpan = xMax + xPad - xLo || 1

    const yMin = Math.min(...ys, 0)
    const yMax = Math.max(...ys, 0)
    const yPad = (yMax - yMin || 1) * 0.12
    const yLo = yMin - yPad
    const ySpan = yMax + yPad - yLo || 1

    const toX = (value: number) => PAD.left + ((value - xLo) / xSpan) * PLOT_W
    const toY = (value: number) => PAD.top + PLOT_H - ((value - yLo) / ySpan) * PLOT_H

    return {
      dots: points.map((point, index) => ({ ...point, index, cx: toX(point.x), cy: toY(point.y) })),
      // The line that actually matters: above it the week gained, below it lost.
      zeroY: toY(0),
      xTicks: [xLo + xPad, (xMin + xMax) / 2, xMax + xPad - xPad],
      yTicks: [yMax + yPad, 0, yLo],
      toY,
    }
  }, [points])

  if (!geometry) {
    return null
  }

  const active = hover !== null ? geometry.dots[hover] : null

  return (
    <figure className="relative m-0">
      <svg
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        className="w-full"
        role="img"
        aria-label={`${xLabel} ve ${yLabel}`}
      >
        {geometry.yTicks.map((tick, index) => (
          <g key={index}>
            <line
              x1={PAD.left}
              x2={WIDTH - PAD.right}
              y1={geometry.toY(tick)}
              y2={geometry.toY(tick)}
              stroke="currentColor"
              className={tick === 0 ? 'text-border-strong' : 'text-border'}
              strokeWidth={1}
            />
            <text
              x={PAD.left - 8}
              y={geometry.toY(tick) + 4}
              textAnchor="end"
              className="fill-current text-text-muted"
              fontSize={11}
            >
              {tick > 0 ? `+${tick.toFixed(2)}` : tick.toFixed(2)}
            </text>
          </g>
        ))}

        {geometry.dots.map((dot) => (
          <circle
            key={dot.index}
            cx={dot.cx}
            cy={dot.cy}
            r={hover === dot.index ? 7 : 5}
            className={`fill-current ${dot.y > 0 ? 'text-accent-orange' : 'text-primary'}`}
            stroke="var(--color-surface)"
            strokeWidth={1.5}
            onMouseEnter={() => setHover(dot.index)}
            onMouseLeave={() => setHover(null)}
          />
        ))}

        <text
          x={PAD.left + PLOT_W / 2}
          y={HEIGHT - 6}
          textAnchor="middle"
          className="fill-current text-text-muted"
          fontSize={11}
        >
          {xLabel} ({xUnit})
        </text>
        <text x={PAD.left} y={HEIGHT - 22} className="fill-current text-text-muted" fontSize={11}>
          {Math.round(Math.min(...points.map((point) => point.x)))}
        </text>
        <text
          x={WIDTH - PAD.right}
          y={HEIGHT - 22}
          textAnchor="end"
          className="fill-current text-text-muted"
          fontSize={11}
        >
          {Math.round(Math.max(...points.map((point) => point.x)))}
        </text>
      </svg>

      {active ? (
        <div
          className="pointer-events-none absolute -translate-x-1/2 -translate-y-full rounded-lg border border-border bg-surface-high px-2.5 py-1.5 text-[11px] shadow-lg"
          style={{ left: `${(active.cx / WIDTH) * 100}%`, top: `${(active.cy / HEIGHT) * 100}%` }}
        >
          <span className="block font-semibold text-text-primary">{active.label}</span>
          <span className="block text-text-muted">
            {Math.round(active.x)} {xUnit} · {active.y > 0 ? '+' : ''}
            {active.y.toFixed(2)} {yUnit}
          </span>
        </div>
      ) : null}

      <figcaption className="mt-2 text-[11px] text-text-muted">
        Her nokta bir hafta. Yatay çizginin üstü kilo artışı, altı kilo kaybı.
      </figcaption>
    </figure>
  )
}
