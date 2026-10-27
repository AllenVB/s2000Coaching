/* S2000 Coaching service worker: lets the app open without a connection.
 *
 * Only the app itself is cached (its HTML shell and built assets). API calls go to another
 * origin and are never touched here: caching them would show one member another's data or a
 * stale plan as if it were current. The member area keeps its own copy of the last plan and
 * its own queue of unsent workouts (src/lib/offline.ts); this file just makes sure the page
 * that reads them can load.
 */
const CACHE = 's2000-shell-v1'

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(['/', '/manifest.webmanifest'])))
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  const url = new URL(request.url)
  if (request.method !== 'GET' || url.origin !== self.location.origin) {
    return
  }

  // A page load: the network first so a fresh deploy is picked up, the cached shell when offline.
  // Every route is the same single-page app, so any path can fall back to the cached '/'.
  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone()
          caches.open(CACHE).then((cache) => cache.put('/', copy))
          return response
        })
        .catch(() => caches.match('/')),
    )
    return
  }

  // Built assets have hashed names, so a cached copy is never stale: serve it, and refresh in the background.
  event.respondWith(
    caches.match(request).then((cached) => {
      const network = fetch(request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone()
            caches.open(CACHE).then((cache) => cache.put(request, copy))
          }
          return response
        })
        .catch(() => cached)
      return cached || network
    }),
  )
})
