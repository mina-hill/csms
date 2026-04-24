import { useState, useCallback } from 'react'
import Login, { readCsmsUser } from './Login.jsx'
import LegacyShell from './LegacyShell.jsx'

export default function App() {
  const [user, setUser] = useState(() => readCsmsUser())

  const onLoggedIn = useCallback((u) => {
    setUser(u)
  }, [])

  if (!user || !user.userId) {
    return <Login onLoggedIn={onLoggedIn} />
  }

  return <LegacyShell key={user.userId} />
}
