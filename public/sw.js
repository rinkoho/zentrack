// ZenTrack PWA Service Worker (Minimal Cache & Pass-through for Local Low-Latency WebSockets)
const CACHE_NAME = 'zentrack-cache-v1';
const ASSETS = [
  '/',
  '/index.html',
  '/style.css',
  '/client.js',
  '/icon.svg',
  '/manifest.json'
];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (e) => {
  // Check if request is a WebSocket connection or API call
  if (e.request.url.startsWith('ws') || e.request.url.includes('/socket.io') || e.request.method !== 'GET') {
    return; // Let the browser handle standard non-GET and WebSockets directly
  }

  // Network falling back to Cache strategy for low-latency updates
  e.respondWith(
    fetch(e.request)
      .then((response) => {
        // Cache the newly fetched asset
        if (response.status === 200) {
          const resClone = response.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(e.request, resClone);
          });
        }
        return response;
      })
      .catch(() => {
        // Fallback to cache if offline / connection drops
        return caches.match(e.request);
      })
  );
});
