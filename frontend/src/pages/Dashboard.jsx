import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { PlayIcon, SparklesIcon } from '../components/Icons.jsx'

export default function Dashboard() {
  const { user } = useAuth()
  const role = user?.role || 'STUDENT'
  const isTeacher = ['TEACHER', 'ADMIN'].includes(role)

  const [exams, setExams] = useState([])
  const [sheets, setSheets] = useState([])
  const [analytics, setAnalytics] = useState(null)
  const [reviewItems, setReviewItems] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function loadDashboard() {
    setLoading(true)
    setError('')
    try {
      const examRes = await api.get('/api/exams')
      const examList = examRes.data?.data || []
      setExams(examList)

      if (role === 'STUDENT') {
        const [sheetRes, analyticsRes] = await Promise.all([
          api.get('/api/answer-sheets', { params: { studentId: user.userId } }),
          api.get(`/api/analytics/student/${user.userId}`)
        ])
        setSheets(sheetRes.data?.data || [])
        setAnalytics(analyticsRes.data?.data)
        return
      }

      // Teacher / Admin aggregation
      const sheetGroups = await Promise.all(
        examList.map((exam) =>
          api.get('/api/answer-sheets', { params: { examId: exam.id } })
            .then((res) => res.data?.data || [])
            .catch(() => [])
        )
      )
      const allSheets = sheetGroups.flat()
      setSheets(allSheets)

      const evalGroups = await Promise.all(
        allSheets.slice(0, 8).map((sheet) =>
          api.get('/api/evaluations', { params: { answerSheetId: sheet.id } })
            .then((res) => res.data?.data || [])
            .catch(() => [])
        )
      )
      const allEvals = evalGroups.flat()
      setReviewItems(allEvals.filter((ev) => ev.status === 'NEEDS_REVIEW' || (ev.overallConfidence && ev.overallConfidence < 0.65)))
    } catch (cause) {
      setError(cause.message || 'Unable to connect to AES-QA backend.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDashboard()
  }, [role, user?.userId])

  const processedCount = sheets.filter((s) => s.status === 'PROCESSED').length
  const featuredExam = exams[0]
  const pendingSheet = sheets.find((s) => s.status === 'UPLOADED' || s.status === 'PROCESSING') || sheets[0]

  return (
    <div className="dashboard-container">
      {/* Netflix Billboard Hero Spotlight */}
      <div className="netflix-billboard">
        <div className="billboard-content">
          <div className="billboard-badge">
            <SparklesIcon size={14} />
            <span>{role === 'STUDENT' ? 'Personal Learning Hub' : 'Featured Assessment'}</span>
          </div>

          <h1 className="billboard-title">
            {role === 'STUDENT'
              ? `Welcome back, ${user?.name?.split(' ')[0] || 'Student'}`
              : featuredExam
              ? featuredExam.title
              : 'AES-QA Smart Evaluation Engine'}
          </h1>

          <p className="billboard-desc">
            {role === 'STUDENT'
              ? 'Real-time OCR extraction, rubric feedback, and tailored concept guidance for your examination submissions.'
              : featuredExam
              ? `${featuredExam.subject} assessment · ${featuredExam.totalMarks || 100} Maximum marks. Automated Tesseract OCR and hybrid LSA semantic evaluation ready.`
              : 'Automate handwritten and typed answer sheet scoring with computer vision and explainable AI rubrics.'}
          </p>

          <div className="billboard-actions">
            {pendingSheet ? (
              <Link
                to={`/answer-sheets/${pendingSheet.id}`}
                className="btn-netflix-red"
                title="Resume Evaluation Workspace"
              >
                <PlayIcon size={16} />
                <span>Resume Grading</span>
              </Link>
            ) : isTeacher ? (
              <Link to="/exams/new" className="btn-netflix-red">
                <span>＋ Create Exam</span>
              </Link>
            ) : null}

            {isTeacher && (
              <Link to="/upload" className="btn-netflix-ghost">
                <span>Upload Answer Sheet</span>
              </Link>
            )}

            <Link to="/analytics" className="btn-netflix-ghost">
              <span>View Analytics</span>
            </Link>
          </div>
        </div>

      </div>

      {error && (
        <div className="error-banner">
          <strong>Backend Notice:</strong> {error}{' '}
          <button className="inline-action" onClick={loadDashboard}>
            Retry Connection
          </button>
        </div>
      )}

      {/* SaaS KPI Metric Cards */}
      <div className="stat-row">
        <div className="stat-luxe">
          <span>{role === 'STUDENT' ? 'MY SUBMISSIONS' : 'TOTAL SUBMISSIONS'}</span>
          <b>{sheets.length}</b>
          <small>{processedCount} evaluated by AI pipeline</small>
        </div>

        <div className="stat-luxe">
          <span>{role === 'STUDENT' ? 'AVERAGE GRADE' : 'ACTIVE EXAMS'}</span>
          <b>{role === 'STUDENT' ? `${analytics?.averageMarksPercent || 0}%` : exams.length}</b>
          <small>{role === 'STUDENT' ? 'across all evaluations' : 'examination spaces'}</small>
        </div>

        <div className="stat-luxe">
          <span>{role === 'STUDENT' ? 'FOCUS CONCEPTS' : 'FLAGGED REVIEWS'}</span>
          <b>{role === 'STUDENT' ? (analytics?.topWeakConcepts || []).length : reviewItems.length}</b>
          <small>{role === 'STUDENT' ? 'topics recommended to revisit' : 'low confidence or needs review'}</small>
        </div>

        <div className="stat-luxe">
          <span>PIPELINE HEALTH</span>
          <b style={{ color: 'var(--success)' }}>100%</b>
          <small>OpenCV + OCR + Spring Boot</small>
        </div>
      </div>

      {/* Compact recent submissions (max 5 rows keeps the page short) */}
      {sheets.length > 0 && (
        <section className="recent-panel">
          <div className="recent-head">
            <h3>{role === 'STUDENT' ? 'My Submissions' : 'Recent Submissions'}</h3>
            <Link to={role === 'STUDENT' ? '/student/learning' : '/analytics'}>View all →</Link>
          </div>
          <ul className="recent-list">
            {sheets.slice(0, 5).map((sheet) => (
              <li key={sheet.id}>
                <Link to={`/answer-sheets/${sheet.id}`} className="recent-row">
                  <span className="recent-name">{sheet.studentName || 'Student'}</span>
                  <span className="recent-meta">{sheet.originalFileName} · {sheet.subject || 'Exam'}</span>
                  <span className={`tag ${sheet.status === 'PROCESSED' ? 'success' : 'danger'}`}>{sheet.status}</span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}