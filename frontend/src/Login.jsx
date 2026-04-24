import { useState } from 'react'
import { joinApiUrl } from './apiBase.js'

const SESSION_KEY = 'csms_user'

/**
 * @typedef {{ userId: string, username: string, email: string, role: string, active: boolean }} CsmsUser
 */

export function readCsmsUser() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function clearCsmsUser() {
  sessionStorage.removeItem(SESSION_KEY)
}

export function saveCsmsUser(/** @type {CsmsUser} */ u) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(u))
}

export default function Login({ onLoggedIn }) {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    const trimmed = email.trim()
    if (!trimmed) {
      setError('Enter your email address.')
      return
    }
    setLoading(true)
    try {
      const url = joinApiUrl('/api/auth/login')
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ email: trimmed }),
      })
      const ct = (res.headers.get('content-type') || '').toLowerCase()
      const text = await res.text()
      let data = {}
      if (text && (ct.includes('application/json') || /^\s*\{/.test(text))) {
        try {
          data = JSON.parse(text)
        } catch {
          data = {}
        }
      } else if (text && res.ok === false) {
        setError(
          `The server did not return JSON (status ${res.status}). Use "npm run dev" so /api is proxied to Spring Boot, or set VITE_API_BASE_URL in .env to your API (e.g. http://127.0.0.1:8080) and restart Vite.`
        )
        return
      }
      if (res.status === 404) {
        let msg = data.message || 'No user found with that email.'
        if (data.userCount != null && data.note) {
          msg = `${msg} (users in this API database: ${data.userCount})`
        }
        setError(msg)
        return
      }
      if (res.status === 403) {
        setError(data.message || 'Account is inactive.')
        return
      }
      if (!res.ok) {
        setError(data.message || 'Sign-in failed. Try again.')
        return
      }
      const user = {
        userId: data.userId,
        username: data.username,
        email: data.email,
        role: data.role,
        active: data.active,
      }
      if (!user.userId || !user.role) {
        setError('Invalid response from server.')
        return
      }
      saveCsmsUser(user)
      onLoggedIn(user)
    } catch {
      setError('Cannot reach the server. Is the API running?')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <div className="login-mark" aria-hidden>
            🐔
          </div>
          <h1>
            Flock<em>Control</em>
          </h1>
          <p className="login-sub">Sign in with your account email. No password is stored in this build.</p>
        </div>
        <form onSubmit={submit} className="login-form">
          <label htmlFor="csms-email">Work email</label>
          <input
            id="csms-email"
            type="email"
            name="email"
            autoComplete="username"
            placeholder="name@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
          />
          {error ? <p className="login-error">{error}</p> : null}
          <button type="submit" className="login-submit" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  )
}
