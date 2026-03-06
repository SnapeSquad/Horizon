import { useState } from "react"
import { AuthForm } from "./components/auth/auth-form"
import { MainDashboard } from "./components/dashboard/main-dashboard"
import { StorePage } from "./components/store/store-page"
import { ForumPage } from "./components/forum/forum-page"
import { AdminPanel } from "./components/admin/admin-panel"

type Page = 'auth' | 'dashboard' | 'store' | 'forum' | 'admin'

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('dashboard')
  const isAuthenticated = true // For demo

  if (!isAuthenticated) {
    return <AuthForm />
  }

  return (
    <>
      {currentPage === 'dashboard' && <MainDashboard />}
      {currentPage === 'store' && <StorePage />}
      {currentPage === 'forum' && <ForumPage />}
      {currentPage === 'admin' && <AdminPanel />}
      
      {/* Navigation for demo */}
      <div className="fixed bottom-4 left-4 flex gap-2">
        {(['dashboard', 'store', 'forum', 'admin'] as Page[]).map((page) => (
          <button
            key={page}
            onClick={() => setCurrentPage(page)}
            className="px-4 py-2 glass-panel text-sm hover:bg-surface/80 transition-colors"
          >
            {page}
          </button>
        ))}
      </div>
    </>
  )
}

export default App
