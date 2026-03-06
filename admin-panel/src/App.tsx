import { useMemo, useState } from "react"
import { AuthForm, type AuthSession } from "./components/auth/auth-form"
import { MainDashboard } from "./components/dashboard/main-dashboard"
import { StorePage } from "./components/store/store-page"
import { ForumPage } from "./components/forum/forum-page"
import { AdminPanel } from "./components/admin/admin-panel"

type Page = "auth" | "dashboard" | "store" | "forum" | "admin"

const SESSION_STORAGE_KEY = "horizon-admin-session"

function readSession(): AuthSession | null {
  const rawValue = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!rawValue) {
    return null
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<AuthSession>
    if (!parsed || typeof parsed.username !== "string") {
      return null
    }
    return {
      username: parsed.username,
      accessToken: typeof parsed.accessToken === "string" ? parsed.accessToken : undefined,
      adminToken: typeof parsed.adminToken === "string" ? parsed.adminToken : undefined,
    }
  } catch {
    return null
  }
}

function App() {
  const [currentPage, setCurrentPage] = useState<Page>("admin")
  const [session, setSession] = useState<AuthSession | null>(() => readSession())

  const isAuthenticated = Boolean(session)

  const sessionLabel = useMemo(() => {
    if (!session) {
      return "guest"
    }
    const suffix = session.adminToken ? " +token" : ""
    return `${session.username}${suffix}`
  }, [session])

  const handleAuthenticated = (nextSession: AuthSession) => {
    setSession(nextSession)
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession))
    setCurrentPage("admin")
  }

  const handleLogout = () => {
    setSession(null)
    localStorage.removeItem(SESSION_STORAGE_KEY)
    setCurrentPage("auth")
  }

  if (!isAuthenticated || !session) {
    return <AuthForm onAuthenticated={handleAuthenticated} />
  }

  return (
    <>
      {currentPage === "dashboard" && <MainDashboard />}
      {currentPage === "store" && <StorePage />}
      {currentPage === "forum" && <ForumPage />}
      {currentPage === "admin" && <AdminPanel session={session} />}

      <div className="fixed bottom-4 left-4 flex gap-2 items-center flex-wrap">
        {(["dashboard", "store", "forum", "admin"] as Page[]).map((page) => (
          <button
            key={page}
            onClick={() => setCurrentPage(page)}
            className="px-4 py-2 glass-panel text-sm hover:bg-surface/80 transition-colors"
          >
            {page}
          </button>
        ))}
        <span className="px-3 py-2 glass-panel text-xs text-text-muted">session: {sessionLabel}</span>
        <button
          onClick={handleLogout}
          className="px-4 py-2 glass-panel text-sm text-red-400 hover:text-red-300 transition-colors"
        >
          logout
        </button>
      </div>
    </>
  )
}

export default App
