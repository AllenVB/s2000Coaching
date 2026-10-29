import { useEffect, useRef, useState } from 'react'

/**
 * A rest countdown started by the member between sets. Counts against the clock rather than
 * decrementing once a second: a phone throttles timers in a locked or backgrounded tab, and a
 * counter that drifts would tell them to lift while they still have a minute left.
 */
export function RestTimer({ seconds }: { seconds: number }) {
  const [endsAt, setEndsAt] = useState<number | null>(null)
  const [remaining, setRemaining] = useState(0)
  const notified = useRef(false)

  useEffect(() => {
    if (endsAt === null) {
      return
    }
    const tick = () => {
      const left = Math.max(0, Math.ceil((endsAt - Date.now()) / 1000))
      setRemaining(left)
      if (left === 0 && !notified.current) {
        notified.current = true
        // A short buzz where the device supports it; silent otherwise.
        navigator.vibrate?.([200, 100, 200])
      }
    }
    tick()
    const handle = window.setInterval(tick, 250)
    return () => window.clearInterval(handle)
  }, [endsAt])

  const running = endsAt !== null && remaining > 0

  const start = () => {
    notified.current = false
    setRemaining(seconds)
    setEndsAt(Date.now() + seconds * 1000)
  }

  if (!running && endsAt !== null && remaining === 0) {
    return (
      <button
        type="button"
        onClick={start}
        className="rounded-lg bg-success/15 px-3 py-1.5 text-xs font-semibold text-success"
      >
        Dinlenme bitti — tekrar başlat
      </button>
    )
  }

  if (running) {
    const minutes = Math.floor(remaining / 60)
    const rest = String(remaining % 60).padStart(2, '0')
    return (
      <button
        type="button"
        onClick={() => setEndsAt(null)}
        aria-label="Sayacı durdur"
        className="rounded-lg bg-accent-orange/15 px-3 py-1.5 font-mono text-xs font-semibold text-accent-orange"
      >
        Dinlenme {minutes}:{rest} · durdur
      </button>
    )
  }

  return (
    <button
      type="button"
      onClick={start}
      className="rounded-lg bg-surface-elevated px-3 py-1.5 text-xs font-semibold text-text-primary transition-colors hover:bg-surface-high"
    >
      Dinlenme sayacı ({seconds} sn)
    </button>
  )
}
