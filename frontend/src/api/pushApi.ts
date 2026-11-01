import { useQuery } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'

import { useMemberAuth } from '@/context/MemberAuthContext'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'

/** The server's VAPID public key as the bytes `pushManager.subscribe` wants; empty means notifications are off. */
function keyToBytes(base64Url: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64Url.length % 4)) % 4)
  const raw = atob((base64Url + padding).replace(/-/g, '+').replace(/_/g, '/'))
  const bytes = new Uint8Array(new ArrayBuffer(raw.length))
  for (let index = 0; index < raw.length; index += 1) {
    bytes[index] = raw.charCodeAt(index)
  }
  return bytes
}

const supported = () => 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window

export type PushState = 'unsupported' | 'off-on-server' | 'denied' | 'enabled' | 'disabled'

/**
 * Phone notifications for this device: whether they can be turned on, whether they are on, and the switch.
 * A device is only subscribed after the member presses the button, never on page load.
 */
export function usePushNotifications() {
  const { session } = useMemberAuth()
  const config = useQuery({
    queryKey: ['push-config'],
    queryFn: () => apiFetch<{ publicKey: string }>('/api/member/push/config', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session) && supported(),
    staleTime: 10 * 60 * 1000,
  })
  const [subscribed, setSubscribed] = useState<boolean | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!supported()) return
    navigator.serviceWorker.getRegistration().then(async (registration) => {
      setSubscribed(Boolean(registration && (await registration.pushManager.getSubscription())))
    })
  }, [])

  const enable = useCallback(async () => {
    setError(null)
    setBusy(true)
    try {
      const key = config.data?.publicKey
      if (!key) throw new Error('Bildirimler şu an kapalı.')
      const permission = await Notification.requestPermission()
      if (permission !== 'granted') throw new Error('Tarayıcı bildirim iznini vermedi.')
      const registration = (await navigator.serviceWorker.getRegistration()) ?? (await navigator.serviceWorker.register('/sw.js'))
      await navigator.serviceWorker.ready
      const subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: keyToBytes(key) })
      const json = subscription.toJSON()
      await apiFetch<void>('/api/member/push/subscription', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify({ endpoint: json.endpoint, p256dh: json.keys?.p256dh, auth: json.keys?.auth }),
      })
      setSubscribed(true)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Bildirimler açılamadı.')
    } finally {
      setBusy(false)
    }
  }, [config.data, session])

  const disable = useCallback(async () => {
    setError(null)
    setBusy(true)
    try {
      const registration = await navigator.serviceWorker.getRegistration()
      const subscription = await registration?.pushManager.getSubscription()
      if (subscription) {
        await apiFetch<void>('/api/member/push/subscription', {
          method: 'DELETE',
          headers: memberHeaders(session!.token),
          body: JSON.stringify({ endpoint: subscription.endpoint }),
        })
        await subscription.unsubscribe()
      }
      setSubscribed(false)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Bildirimler kapatılamadı.')
    } finally {
      setBusy(false)
    }
  }, [session])

  let state: PushState = 'disabled'
  if (!supported()) state = 'unsupported'
  else if (config.data && config.data.publicKey === '') state = 'off-on-server'
  else if (typeof Notification !== 'undefined' && Notification.permission === 'denied') state = 'denied'
  else if (subscribed) state = 'enabled'

  return { state, loading: config.isLoading || subscribed === null, enable, disable, busy, error }
}

/** Chrome/Edge offer an install prompt through an event; it is kept until the member presses the button. */
interface InstallEvent extends Event {
  prompt: () => Promise<void>
}

export function useInstallPrompt() {
  const [event, setEvent] = useState<InstallEvent | null>(null)
  const standalone = typeof window !== 'undefined' && window.matchMedia('(display-mode: standalone)').matches
  const ios = typeof navigator !== 'undefined' && /iphone|ipad|ipod/i.test(navigator.userAgent)

  useEffect(() => {
    const capture = (raw: Event) => {
      raw.preventDefault()
      setEvent(raw as InstallEvent)
    }
    window.addEventListener('beforeinstallprompt', capture)
    return () => window.removeEventListener('beforeinstallprompt', capture)
  }, [])

  return {
    installed: standalone,
    canPrompt: event !== null,
    showIosHint: ios && !standalone,
    install: async () => {
      await event?.prompt()
      setEvent(null)
    },
  }
}
