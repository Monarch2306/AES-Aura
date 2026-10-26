import { useEffect, useState } from 'react'
import api from '../api/client'
import { EmptyState, ErrorState } from '../components/AsyncState.jsx'

function Metric({ label, value }) { return <div><span>{label}</span><b>{value ?? 'N/A'}</b></div> }

export default function AdminDashboard() {
  const [dashboard, setDashboard] = useState(null)
  const [health, setHealth] = useState(null)
  const [users, setUsers] = useState([])
  const [logs, setLogs] = useState([])
  const [search, setSearch] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([api.get('/api/admin/dashboard'), api.get('/api/admin/system-health'), api.get('/api/admin/users'), api.get('/api/admin/audit-logs')])
      .then(([dashboardResponse, healthResponse, usersResponse, logsResponse]) => { setDashboard(dashboardResponse.data.data); setHealth(healthResponse.data.data); setUsers(usersResponse.data.data || []); setLogs(logsResponse.data.data || []) })
      .catch((cause) => setError(cause.message || 'Unable to load admin console.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="analytics-grid analytics-loading"><div className="surface"><div className="skeleton-line wide"/><div className="skeleton-line"/><div className="skeleton-line"/></div><div className="surface"><div className="skeleton-chart"/></div></div>
  if (error) return <ErrorState title="Admin console unavailable" message={error}/>
  if (!dashboard) return <EmptyState title="No system data" message="The admin dashboard has no data to display."/>
  const visibleUsers = users.filter((user) => !search || `${user.name} ${user.email} ${user.role}`.toLowerCase().includes(search.toLowerCase()))

  async function toggleStatus(userId, currentStatus) {
    const nextStatus = currentStatus === 'DEACTIVATED' ? 'ACTIVE' : 'DEACTIVATED'
    try {
      const res = await api.patch(`/api/admin/users/${userId}/status`, { status: nextStatus })
      setUsers(users.map(u => u.id === userId ? res.data.data : u))
    } catch (cause) {
      alert(cause.message)
    }
  }

  return <div><div className="detail-hero"><div><span className="eyebrow">ADMINISTRATION</span><h1 className="page-title">System console</h1><p className="lead">Operational data from the current AES-QA installation.</p></div></div><div className="analytics-cards"><Metric label="USERS" value={dashboard.users}/><Metric label="EXAMS" value={dashboard.exams}/><Metric label="ANSWER SHEETS" value={dashboard.answerSheets}/><Metric label="EVALUATIONS" value={dashboard.evaluations}/><Metric label="PENDING REVIEWS" value={dashboard.pendingReviews}/><Metric label="FAILED EVALUATIONS" value={dashboard.failedEvaluations}/><Metric label="SIMILARITY FLAGS" value={dashboard.similarityFlags}/><Metric label="CONSISTENCY FLAGS" value={dashboard.consistencyFlags}/></div><section className="surface"><div className="section-title"><div><span className="eyebrow">SYSTEM HEALTH</span><h2>Live service status</h2></div><span className="tag success">{health?.backend || 'ONLINE'}</span></div><div className="metric-list"><div><span>Database</span><b>{health?.database || 'N/A'}</b></div><div><span>AI service</span><b>{health?.aiService || 'N/A'}</b></div><div><span>OCR</span><b>{health?.ocr || 'N/A'}</b></div><div><span>Evaluation API</span><b>{health?.evaluationApi || 'N/A'}</b></div><div><span>Storage</span><b>{health?.storage || 'N/A'}</b></div><div><span>Environment</span><b>{health?.environment || 'N/A'}</b></div></div><p className="muted">Last checked {health?.checkedAt ? new Date(health.checkedAt).toLocaleString() : 'Just now'} · Version {health?.applicationVersion || '1.0.0'}</p></section><section className="surface"><div className="section-title"><div><span className="eyebrow">USER MANAGEMENT</span><h2>Authorized users</h2></div><input className="input-luxe" placeholder="Search name, email, role" value={search} onChange={(event) => setSearch(event.target.value)}/></div>{visibleUsers.length ? <div className="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Action</th></tr></thead><tbody>{visibleUsers.map((user) => <tr key={user.id}><td>{user.name}</td><td>{user.email}</td><td>{user.role}</td><td><span className={`tag ${user.status === 'DEACTIVATED' ? 'error' : 'success'}`}>{user.status || 'ACTIVE'}</span></td><td><button className="ghost-btn small" onClick={() => toggleStatus(user.id, user.status)}>{user.status === 'DEACTIVATED' ? 'Activate' : 'Deactivate'}</button></td></tr>)}</tbody></table></div> : <EmptyState title="No matching users" message="Try a different search."/>}</section><section className="surface"><div className="section-title"><div><span className="eyebrow">AUDIT LOG</span><h2>Recent events</h2></div></div>{logs.length ? <div className="table-wrap"><table><thead><tr><th>Action</th><th>Entity</th><th>Actor</th><th>Timestamp</th></tr></thead><tbody>{logs.map((log) => <tr key={log.id}><td>{log.action}</td><td>{log.entityType} {log.entityId || ''}</td><td>{log.actorUserId || 'System'}</td><td>{new Date(log.timestamp).toLocaleString()}</td></tr>)}</tbody></table></div> : <EmptyState title="No audit events" message="No audit records are stored yet."/>}</section></div>
}
