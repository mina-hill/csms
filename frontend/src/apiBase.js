/**
 * In dev, leave VITE_API_BASE_URL empty so /api is proxied by Vite to Spring Boot.
 * For static hosting, set VITE_API_BASE_URL to your API root (e.g. http://127.0.0.1:8080) — no trailing slash.
 */
export function apiBaseUrl() {
  const b = (import.meta.env.VITE_API_BASE_URL ?? '').trim()
  return b.replace(/\/$/, '')
}

/** Path must start with / (e.g. /api/auth/login) */
export function joinApiUrl(path) {
  const p = path.startsWith('/') ? path : `/${path}`
  const base = apiBaseUrl()
  if (!base) return p
  return `${base}${p}`
}
