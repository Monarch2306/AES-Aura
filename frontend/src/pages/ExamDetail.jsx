import React, { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { ErrorState, LoadingState } from '../components/AsyncState.jsx'
import {
  SparklesIcon,
  DownloadIcon,
  SearchIcon,
  PlayIcon,
  CheckIcon
} from '../components/Icons.jsx'

function QuestionRow({ question, onSaved, canEdit }) {
  const [open, setOpen] = useState(false)
  const [reference, setReference] = useState('')
  const [concepts, setConcepts] = useState('')
  const [minimum, setMinimum] = useState('0')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function save(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.post('/api/rubrics', {
        questionId: question.id,
        referenceAnswer: reference,
        concepts: concepts
          .split('\n')
          .filter(Boolean)
          .map((line) => {
            const parts = line.split(',').map((p) => p.trim())
            return {
              concept: parts[0],
              weight: parts[1] ? Number(parts[1]) : 1,
              required: parts.includes('required'),
              negative: parts.includes('negative')
            }
          }),
        minRequiredConcepts: Number(minimum) || 0
      })
      setOpen(false)
      onSaved()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="question-row" style={{ padding: '20px 0', borderTop: '1px solid var(--line)' }}>
      <div className="q-index" style={{ width: '42px', height: '42px', borderRadius: '12px' }}>
        Q{question.questionNumber}
      </div>
      <div className="q-body" style={{ flex: 1 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '16px' }}>
          <div>
            <h4 style={{ margin: '0 0 6px', font: '700 16px Manrope', color: '#fff' }}>{question.text}</h4>
            <span style={{ fontSize: '12px', color: 'var(--text-subtle)' }}>
              Allocated Weight: <b>{question.maxMarks} marks</b>
            </span>
          </div>
          {question.hasRubric ? (
            <span className="tag success">✓ RUBRIC CONFIGURED</span>
          ) : (
            canEdit && (
              <button
                className="ghost-btn"
                style={{ padding: '6px 12px', fontSize: '12px' }}
                onClick={() => setOpen(!open)}
              >
                {open ? 'Cancel' : '+ Define Rubric'}
              </button>
            )
          )}
        </div>

        {question.hasRubric ? (
          <div style={{ display: 'flex', gap: '16px', marginTop: '10px', fontSize: '11.5px', color: '#34d399' }}>
            <span>✓ Reference Answer Model Set</span>
            <span>✓ Semantic LSA + Concept Keywords Active</span>
          </div>
        ) : (
          canEdit && open && (
            <form
              onSubmit={save}
              style={{
                marginTop: '16px',
                padding: '18px',
                borderRadius: '14px',
                background: '#090a0f',
                border: '1px solid var(--line)',
                display: 'grid',
                gap: '14px'
              }}
            >
              {error && <div className="error-banner">{error}</div>}
              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '6px' }}>
                  Model Reference Answer
                </label>
                <textarea
                  rows="3"
                  value={reference}
                  onChange={(e) => setReference(e.target.value)}
                  style={{ width: '100%' }}
                  placeholder="Enter ideal solution text for semantic comparison..."
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '6px' }}>
                  Keywords & Concepts (One per line: concept, weight, [required/negative])
                </label>
                <textarea
                  rows="3"
                  value={concepts}
                  onChange={(e) => setConcepts(e.target.value)}
                  style={{ width: '100%' }}
                  placeholder={'photosynthesis, 1.5, required\nchloroplast, 1.0\nglucose, 1.0'}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '11px', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '6px' }}>
                  Minimum Required Concepts for Passing
                </label>
                <input
                  type="number"
                  min="0"
                  value={minimum}
                  onChange={(e) => setMinimum(e.target.value)}
                  style={{ width: '120px' }}
                />
              </div>

              <button className="btn-netflix-red" type="submit" disabled={busy} style={{ width: 'fit-content' }}>
                {busy ? 'Saving Rubric...' : 'Save Rubric Model'}
              </button>
            </form>
          )
        )}
      </div>
    </div>
  )
}

export default function ExamDetail() {
  const { examId } = useParams()
  const { user } = useAuth()
  const canEdit = ['TEACHER', 'ADMIN'].includes(user?.role)

  const [exam, setExam] = useState(null)
  const [questions, setQuestions] = useState([])
  const [sheets, setSheets] = useState([])
  const [searchQuery, setSearchQuery] = useState('')
  const [form, setForm] = useState({ n: '', text: '', marks: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [batchBusy, setBatchBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    Promise.all([
      api.get(`/api/exams/${examId}`),
      api.get('/api/questions', { params: { examId } }),
      api.get('/api/answer-sheets', { params: { examId } })
    ])
      .then(([examRes, qRes, sRes]) => {
        setExam(examRes.data?.data)
        setQuestions(qRes.data?.data || [])
        setSheets(sRes.data?.data || [])
      })
      .catch((cause) => setError(cause.message || 'Unable to load assessment details.'))
      .finally(() => setLoading(false))
  }, [examId])

  useEffect(() => {
    load()
  }, [load])

  async function addQuestion(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.post('/api/questions', {
        examId: Number(examId),
        questionNumber: Number(form.n),
        text: form.text,
        maxMarks: Number(form.marks)
      })
      setForm({ n: '', text: '', marks: '' })
      load()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusy(false)
    }
  }

  async function handleBatchEvaluate() {
    setBatchBusy(true)
    try {
      await api.post(`/api/evaluations/batch?examId=${examId}`)
      load()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBatchBusy(false)
    }
  }

  async function handleExportCsv() {
    try {
      const res = await api.get(`/api/analytics/exam/${examId}/export?format=csv`, { responseType: 'blob' })
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/csv' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `exam_${examId}_report.csv`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message || 'Export failed.')
    }
  }

  async function handleExportReport() {
    try {
      const res = await api.get(`/api/analytics/exam/${examId}/export?format=txt`, { responseType: 'blob' })
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'text/plain' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `exam_${examId}_report.txt`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message || 'Export failed.')
    }
  }

  if (loading && !exam) return <LoadingState label="Loading assessment workspace..." className="detail-loading" />
  if (!exam) return <ErrorState title="Assessment unavailable" message={error} onRetry={load} />

  const filteredSheets = sheets.filter(
    (s) =>
      s.studentName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.originalFileName?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="exam-detail-page">
      {/* Header Banner */}
      <div
        className="netflix-billboard"
        style={{ minHeight: '260px', padding: '36px', marginBottom: '28px' }}
      >
        <div className="billboard-content">
          <div className="billboard-badge">
            <span>EXAMINATION ASSESSMENT</span>
          </div>
          <h1 className="billboard-title" style={{ fontSize: '38px', margin: '6px 0 10px' }}>
            {exam.title}
          </h1>
          <p className="billboard-desc" style={{ margin: '0 0 20px' }}>
            Subject: <b>{exam.subject}</b> · Total Marks: <b>{exam.totalMarks || 100}</b> · Date: {exam.examDate || 'Ongoing'}
          </p>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            {canEdit && (
              <button
                className="btn-netflix-red"
                onClick={handleBatchEvaluate}
                disabled={batchBusy}
                title="Evaluate all unprocessed submissions in parallel"
              >
                <SparklesIcon size={16} />
                <span>{batchBusy ? 'Evaluating Cohort...' : '⚡ Batch Evaluate All'}</span>
              </button>
            )}
            {canEdit && (
              <button className="btn-netflix-ghost" onClick={handleExportCsv}>
                <DownloadIcon size={16} />
                <span>Export CSV</span>
              </button>
            )}
            {canEdit && (
              <button className="btn-netflix-ghost" onClick={handleExportReport}>
                <DownloadIcon size={16} />
                <span>Export Report</span>
              </button>
            )}
            {canEdit && (
              <Link to="/upload" className="btn-netflix-ghost">
                <span>Upload Sheet →</span>
              </Link>
            )}
            <Link to={`/exams/${examId}/analytics`} className="btn-netflix-ghost">
              <span>Exam Analytics</span>
            </Link>
          </div>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {/* Question Set & Rubrics */}
      <div className="surface" style={{ borderRadius: '24px', marginBottom: '24px' }}>
        <div className="section-title">
          <div>
            <span className="eyebrow">QUESTION BANK & RUBRIC ARCHITECTURE</span>
            <h2>Assessment Questions</h2>
          </div>
          <span className="tag success">{questions.length} DEFINED</span>
        </div>

        {canEdit && (
          <form
            onSubmit={addQuestion}
            style={{
              display: 'grid',
              gridTemplateColumns: '90px minmax(0, 1fr) 110px auto',
              gap: '12px',
              margin: '20px 0 24px',
              padding: '16px',
              background: '#090a0f',
              borderRadius: '16px',
              border: '1px solid var(--line)'
            }}
          >
            <input
              type="number"
              min="1"
              placeholder="Q#"
              value={form.n}
              onChange={(e) => setForm({ ...form, n: e.target.value })}
              required
            />
            <input
              type="text"
              placeholder="Question prompt or title..."
              value={form.text}
              onChange={(e) => setForm({ ...form, text: e.target.value })}
              required
            />
            <input
              type="number"
              min="1"
              step="0.5"
              placeholder="Marks"
              value={form.marks}
              onChange={(e) => setForm({ ...form, marks: e.target.value })}
              required
            />
            <button className="btn-netflix-red" type="submit" disabled={busy}>
              + Add
            </button>
          </form>
        )}

        <div className="question-list">
          {questions.map((q) => (
            <QuestionRow key={q.id} question={q} onSaved={load} canEdit={canEdit} />
          ))}
        </div>
      </div>

      {/* Submissions Table with Live Search */}
      <div className="surface" style={{ borderRadius: '24px' }}>
        <div className="section-title" style={{ marginBottom: '18px' }}>
          <div>
            <span className="eyebrow">COHORT EVIDENCE</span>
            <h2>Submitted Answer Sheets ({sheets.length})</h2>
          </div>

          <div style={{ position: 'relative', minWidth: '220px' }}>
            <input
              type="text"
              placeholder="Search student or file..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ width: '100%', paddingLeft: '36px', height: '38px', fontSize: '12px' }}
            />
            <span style={{ position: 'absolute', left: '12px', top: '10px', color: 'var(--text-muted)' }}>
              <SearchIcon size={16} />
            </span>
          </div>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Student</th>
                <th>File Reference</th>
                <th>Status</th>
                <th>OCR Confidence</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredSheets.length > 0 ? (
                filteredSheets.map((s) => (
                  <tr key={s.id}>
                    <td>
                      <b style={{ color: '#fff' }}>{s.studentName || 'Student'}</b>
                    </td>
                    <td style={{ color: 'var(--text-muted)' }}>{s.originalFileName}</td>
                    <td>
                      <span className={`tag ${s.status === 'PROCESSED' ? 'success' : 'danger'}`}>
                        {s.status}
                      </span>
                    </td>
                    <td>
                      {s.ocrConfidence ? `${Math.round(s.ocrConfidence * 100)}%` : '—'}
                    </td>
                    <td>
                      <Link
                        to={`/answer-sheets/${s.id}`}
                        className="btn-netflix-ghost"
                        style={{ padding: '6px 14px', fontSize: '12px' }}
                      >
                        Inspect & Grade →
                      </Link>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
                    No answer sheets found for this criteria.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}