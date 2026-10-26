import React, { useEffect, useState } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import WhatsAppCopilotDrawer from './WhatsAppCopilotDrawer.jsx'
import {
  BellIcon,
  SearchIcon,
  SparklesIcon,
  CloseIcon
} from './Icons.jsx'

export default function AppShell({ children }) {
  const { user, logout } = useAuth()
  const loc = useLocation()
  const navigate = useNavigate()
  const role = user?.role || 'STUDENT'
  const isTeacher = ['TEACHER', 'ADMIN'].includes(role)

  const navItems = role === 'ADMIN'
    ? [
        { to: '/', icon: '⌂', label: 'Overview' },
        { to: '/admin/dashboard', icon: '▦', label: 'Admin Console' },
        { to: '/analytics', icon: '◫', label: 'Analytics' }
      ]
    : isTeacher
    ? [
        { to: '/', icon: '⌂', label: 'Overview' },
        { to: '/exams/new', icon: '＋', label: 'Create Exam' },
        { to: '/upload', icon: '↥', label: 'Upload Sheet' },
        { to: '/analytics', icon: '◫', label: 'Analytics' }
      ]
    : [
        { to: '/', icon: '⌂', label: 'Overview' },
        { to: '/analytics', icon: '◫', label: 'Analytics' },
        { to: '/student/learning', icon: '✦', label: 'Learning Paths' }
      ]

  const [paletteOpen, setPaletteOpen] = useState(false)
  const [paletteQuery, setPaletteQuery] = useState('')
  const [notifOpen, setNotifOpen] = useState(false)
  const [notifications, setNotifications] = useState([
    { id: 1, text: 'Evaluation pipeline initialized on Port 8080 & 8001', time: '10m ago', unread: true },
    { id: 2, text: 'Real-time WebSocket engine connected (/ws/evaluations)', time: '25m ago', unread: false }
  ])

  useEffect(() => {
    const onKeyDown = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setPaletteOpen(true)
      }
      if (event.key === 'Escape') {
        setPaletteOpen(false)
        setNotifOpen(false)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  const unreadNotifs = notifications.filter((n) => n.unread).length

  return (
    <div className="app-shell">
      {/* Netflix-Style Rail Sidebar */}
      <aside className="rail">
        <div className="brand">
          <div className="brand-mark">N</div>
          <div>
            <strong>AES-QA</strong>
            <small>Intelligence SaaS</small>
          </div>
        </div>

        <nav aria-label="Primary navigation">
          {navItems.map(({ to, icon, label }) => (
            <NavLink
              end={to === '/'}
              key={to}
              to={to}
              title={label}
              className={({ isActive }) => (isActive ? 'active' : '')}
            >
              <span>{icon}</span>
              {label}
            </NavLink>
          ))}
        </nav>

        <div className="rail-bottom">
          <div className="status-pill">
            <i />
            <span>AI Engine Online</span>
          </div>

          <button className="profile" onClick={logout} title="Click to Sign Out">
            <span className="avatar">
              {(user?.name || 'U').slice(0, 1).toUpperCase()}
            </span>
            <span>
              <b>{user?.name || 'Active User'}</b>
              <small>{role} · Sign Out</small>
            </span>
            <em aria-hidden="true">↗</em>
          </button>
        </div>
      </aside>

      {/* Main Workspace */}
      <main className="workspace">
        <header className="topbar">
          <div>
            <span className="crumb">
              AES-QA / {loc.pathname === '/' ? 'OVERVIEW' : loc.pathname.replaceAll('/', ' / ').toUpperCase()}
            </span>
          </div>

          <div className="top-actions">
            <button
              className="search-trigger"
              onClick={() => {
                setPaletteQuery('')
                setPaletteOpen(true)
              }}
              aria-label="Search or press Ctrl K"
            >
              <SearchIcon size={16} />
              <span>Search workspace...</span>
              <kbd>Ctrl K</kbd>
            </button>

            {/* Notification Center Trigger */}
            <div style={{ position: 'relative' }}>
              <button
                className="icon-btn"
                title="Notifications"
                onClick={() => setNotifOpen((v) => !v)}
                aria-label="View notifications"
              >
                <BellIcon size={18} />
                {unreadNotifs > 0 && <span className="icon-badge">{unreadNotifs}</span>}
              </button>

              {/* Notification Center Popover */}
              {notifOpen && (
                <div
                  style={{
                    position: 'absolute',
                    top: '48px',
                    right: '0',
                    width: '320px',
                    background: '#121520',
                    border: '1px solid var(--line)',
                    borderRadius: '16px',
                    boxShadow: '0 20px 50px rgba(0,0,0,0.6)',
                    padding: '16px',
                    zIndex: 50
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                    <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 800 }}>Notifications</h4>
                    <button
                      style={{ fontSize: '11px', color: 'var(--netflix-red)' }}
                      onClick={() => setNotifications((list) => list.map((n) => ({ ...n, unread: false })))}
                    >
                      Mark all read
                    </button>
                  </div>
                  <div style={{ display: 'grid', gap: '8px' }}>
                    {notifications.map((n) => (
                      <div
                        key={n.id}
                        style={{
                          padding: '10px 12px',
                          borderRadius: '10px',
                          background: n.unread ? 'rgba(229,9,20,0.08)' : 'rgba(255,255,255,0.02)',
                          border: '1px solid var(--line)',
                          fontSize: '12px'
                        }}
                      >
                        <p style={{ margin: '0 0 4px', color: '#e5e7eb' }}>{n.text}</p>
                        <small style={{ color: 'var(--text-subtle)', fontSize: '10px' }}>{n.time}</small>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <span className="top-avatar">
              {(user?.name || 'U').slice(0, 1).toUpperCase()}
            </span>
          </div>
        </header>

        <section className="page-content">{children}</section>
      </main>

      {/* Floating WhatsApp AI Copilot Drawer */}
      <WhatsAppCopilotDrawer />

      {/* Command Palette Modal */}
      {paletteOpen && (
        <div className="wa-drawer-backdrop" onClick={() => setPaletteOpen(false)}>
          <div
            style={{
              width: 'min(580px, calc(100vw - 32px))',
              background: '#121520',
              border: '1px solid var(--line)',
              borderRadius: '20px',
              boxShadow: '0 24px 80px rgba(0,0,0,0.7)',
              overflow: 'hidden',
              margin: '12vh auto auto'
            }}
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '18px 20px', borderBottom: '1px solid var(--line)' }}>
              <SearchIcon size={20} />
              <input
                autoFocus
                value={paletteQuery}
                onChange={(e) => setPaletteQuery(e.target.value)}
                placeholder="Jump to exam, evaluation, submission, or action..."
                style={{ width: '100%', background: 'transparent', border: 'none', color: '#fff', fontSize: '15px', outline: 'none' }}
                onKeyDown={(e) => {
                  if (e.key === 'Escape') setPaletteOpen(false)
                }}
              />
              <button onClick={() => setPaletteOpen(false)} style={{ color: 'var(--text-muted)' }}>
                <CloseIcon size={18} />
              </button>
            </div>

            <div style={{ padding: '12px', display: 'grid', gap: '4px', maxHeight: '340px', overflowY: 'auto' }}>
              {navItems
                .filter(({ label }) => label.toLowerCase().includes(paletteQuery.toLowerCase()))
                .map(({ to, icon, label }) => (
                  <button
                    key={to}
                    onClick={() => {
                      navigate(to)
                      setPaletteOpen(false)
                    }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '12px 14px',
                      borderRadius: '10px',
                      color: '#f3f5f8',
                      fontSize: '14px',
                      textAlign: 'left',
                      transition: 'background 0.2s'
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(229,9,20,0.12)')}
                    onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                  >
                    <span style={{ fontSize: '18px', color: 'var(--netflix-red)', width: '22px' }}>{icon}</span>
                    <span style={{ flex: 1 }}>{label}</span>
                    <kbd style={{ fontSize: '11px', color: 'var(--text-muted)', background: 'rgba(255,255,255,0.06)', padding: '3px 6px', borderRadius: '4px' }}>↵</kbd>
                  </button>
                ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}