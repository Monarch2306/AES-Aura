import React, { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { authenticatedImageUrl } from '../utils/fileUrl.js'
import ProcessingVisualizer from '../components/ProcessingVisualizer.jsx'
import { ErrorState } from '../components/AsyncState.jsx'
import { useEvaluationSocket } from '../api/useEvaluationSocket.js'
import {
  ZoomInIcon,
  ZoomOutIcon,
  RotateIcon,
  ContrastIcon,
  SparklesIcon,
  CheckIcon,
  WhatsAppIcon
} from '../components/Icons.jsx'

function QualityPanel({ sheet }) {
  if (sheet.imageQualityScore == null) return null
  const warnings = sheet.imageQualityWarnings || []
  return (
    <section className="surface quality-panel" aria-live="polite">
      <div className="section-title">
        <div>
          <span className="eyebrow">IMAGE QUALITY ANALYSIS</span>
          <h2>{warnings.length ? 'Manual Review Recommended' : 'High Image Fidelity'}</h2>
        </div>
        <span className={`tag ${warnings.length ? 'danger' : 'success'}`}>
          {Math.round(sheet.imageQualityScore)} / 100
        </span>
      </div>
      <div className="quality-metrics">
        <span>Blur <b>{Math.round(sheet.imageBlurScore)}%</b></span>
        <span>Brightness <b>{Math.round(sheet.imageBrightnessScore)}%</b></span>
        <span>Contrast <b>{Math.round(sheet.imageContrastScore)}%</b></span>
        <span>Resolution <b>{sheet.imageResolutionWidth} × {sheet.imageResolutionHeight}</b></span>
      </div>
      {warnings.length > 0 && (
        <ul className="quality-warnings">
          {warnings.map((w) => (
            <li key={w}>{w}</li>
          ))}
        </ul>
      )}
    </section>
  )
}

function CopilotPanel({ evaluation }) {
  const [copilot, setCopilot] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    api.get(`/api/evaluations/${evaluation.id}/copilot`)
      .then((res) => {
        if (active) setCopilot(res.data.data)
      })
      .catch((cause) => {
        if (active) setError(cause.message)
      })
    return () => { active = false }
  }, [evaluation.id])

  if (error) return null
  if (!copilot) return null

  return (
    <div
      style={{
        marginTop: '16px',
        padding: '16px',
        borderRadius: '12px',
        background: 'rgba(37, 211, 102, 0.05)',
        border: '1px solid rgba(37, 211, 102, 0.2)'
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
        <span style={{ color: 'var(--wa-green)' }}><SparklesIcon size={16} /></span>
        <strong style={{ fontSize: '13px', color: '#fff' }}>Copilot Diagnosis: {copilot.recommendation?.replace('_', ' ')}</strong>
        <span className="tag" style={{ marginLeft: 'auto', background: 'rgba(255,255,255,0.06)' }}>
          {copilot.answerQuality}
        </span>
      </div>
      <p style={{ margin: '0 0 10px', fontSize: '12.5px', color: 'var(--text-muted)', lineHeight: '1.6' }}>
        {copilot.explanation}
      </p>
      {copilot.suggestedFeedback && (
        <p style={{ margin: 0, fontSize: '12px', color: '#a7f3d0' }}>
          <b>Suggested Feedback:</b> "{copilot.suggestedFeedback}"
        </p>
      )}
    </div>
  )
}

function EvaluationCard({ evaluation, canReview, onChanged }) {
  const [editing, setEditing] = useState(false)
  const [marks, setMarks] = useState(evaluation.marksAwarded)
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const confidence = Math.round((evaluation.overallConfidence || 0) * 100)

  async function review(action) {
    setBusy(true)
    setError('')
    try {
      await api.post(`/api/evaluations/${evaluation.id}/review`, { action })
      onChanged()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusy(false)
    }
  }

  async function override(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.post(`/api/evaluations/${evaluation.id}/override`, {
        finalMarks: Number(marks),
        overrideReason: reason || 'Teacher adjustment'
      })
      setEditing(false)
      onChanged()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <article className="evaluation-card">
      <div className="eval-head">
        <div className="q-index">Q{evaluation.questionNumber}</div>
        <div>
          <h3 style={{ margin: 0, fontSize: '16px' }}>Question {evaluation.questionNumber}</h3>
          <span style={{ fontSize: '11px', color: 'var(--text-subtle)' }}>
            Max Marks: {evaluation.maxMarks}
          </span>
        </div>
        <div className="score-big">
          <b>{evaluation.finalMarks ?? evaluation.marksAwarded}</b>
          <small>/ {evaluation.maxMarks}</small>
        </div>
        <span className={`tag ${confidence < 60 ? 'danger' : 'success'}`}>
          {confidence}% confidence
        </span>
      </div>

      <div style={{ display: 'grid', gap: '14px', marginTop: '18px' }}>
        <div>
          <span className="eyebrow">EXTRACTED STUDENT ANSWER</span>
          <p
            style={{
              marginTop: '6px',
              padding: '14px',
              borderRadius: '10px',
              background: '#090a0f',
              border: '1px solid var(--line)',
              color: '#d1d5db',
              fontSize: '13px',
              lineHeight: '1.6'
            }}
          >
            {evaluation.studentAnswerText || 'No OCR text recognized for this question segment.'}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', fontSize: '12px', color: 'var(--text-muted)' }}>
          <span>Semantic Match: <b>{Math.round((evaluation.semanticSimilarity || 0) * 100)}%</b></span>
          <span>Concept Coverage: <b>{Math.round((evaluation.conceptCoverage || 0) * 100)}%</b></span>
          <span>OCR Accuracy: <b>{Math.round(evaluation.ocrConfidence || 0)}%</b></span>
        </div>

        <div>
          <span className="eyebrow">CONCEPT EVIDENCE</span>
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: '8px' }}>
            {(evaluation.matchedConcepts || []).map((c) => (
              <span className="tag success" key={`match-${c}`}>✓ {c}</span>
            ))}
            {(evaluation.missingConcepts || []).map((c) => (
              <span className="tag" key={`miss-${c}`}>− {c}</span>
            ))}
            {(evaluation.contradictedConcepts || []).map((c) => (
              <span className="tag danger" key={`contra-${c}`}>! {c}</span>
            ))}
          </div>
        </div>

        {evaluation.feedback && (
          <div style={{ padding: '12px', borderRadius: '10px', background: 'rgba(140,82,255,0.06)', borderLeft: '3px solid var(--accent)' }}>
            <span style={{ fontSize: '11px', fontWeight: 800, color: 'var(--accent)' }}>AI EVALUATION FEEDBACK</span>
            <p style={{ margin: '4px 0 0', fontSize: '12.5px', color: '#e2e8f0', lineHeight: '1.5' }}>
              {evaluation.feedback}
            </p>
          </div>
        )}

        <CopilotPanel evaluation={evaluation} />
      </div>

      {error && <div className="error-banner">{error}</div>}

      {canReview && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '18px', paddingTop: '16px', borderTop: '1px solid var(--line)' }}>
          <span className="tag">{evaluation.status}</span>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button className="ghost-btn" style={{ padding: '8px 14px', fontSize: '12px' }} onClick={() => review('APPROVE')} disabled={busy}>
              <CheckIcon size={14} /> Approve Score
            </button>
            <button className="lux-btn" style={{ padding: '8px 14px', fontSize: '12px' }} onClick={() => setEditing(!editing)}>
              {editing ? 'Close' : 'Override Marks'}
            </button>
          </div>
        </div>
      )}

      {editing && (
        <form onSubmit={override} style={{ display: 'flex', gap: '10px', marginTop: '14px', flexWrap: 'wrap' }}>
          <input
            type="number"
            min="0"
            max={evaluation.maxMarks}
            step="0.5"
            value={marks}
            onChange={(e) => setMarks(e.target.value)}
            style={{ width: '90px' }}
            required
          />
          <input
            type="text"
            placeholder="Reason for mark override..."
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            style={{ flex: 1, minWidth: '180px' }}
            required
          />
          <button className="lux-btn" type="submit" disabled={busy}>
            {busy ? 'Saving...' : 'Save Override'}
          </button>
        </form>
      )}
    </article>
  )
}

export default function EvaluationWorkspace() {
  const { id } = useParams()
  const { user } = useAuth()
  const canReview = ['TEACHER', 'ADMIN'].includes(user?.role)

  const [sheet, setSheet] = useState(null)
  const [evaluations, setEvaluations] = useState([])
  const [image, setImage] = useState('')
  const [pages, setPages] = useState([])
  const [currentPage, setCurrentPage] = useState(1)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)

  // Viewer state
  const [zoom, setZoom] = useState(1)
  const [rotation, setRotation] = useState(0)
  const [invertFilter, setInvertFilter] = useState(false)
  const [activeTab, setActiveTab] = useState('image')

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    Promise.all([
      api.get(`/api/answer-sheets/${id}`),
      api.get('/api/evaluations', { params: { answerSheetId: id } }),
      api.get(`/api/answer-sheets/${id}/pages`).catch(() => ({ data: { data: [] } }))
    ])
      .then(([sheetRes, evalRes, pagesRes]) => {
        setSheet(sheetRes.data?.data || sheetRes.data)
        setEvaluations(evalRes.data?.data || evalRes.data || [])
        setPages(pagesRes.data?.data || [])
      })
      .catch((cause) => setError(cause.message || 'Unable to load answer sheet.'))
      .finally(() => setLoading(false))
  }, [id])

  const { progressEvent } = useEvaluationSocket(id, () => {
    load()
  })

  useEffect(() => {
    load()
  }, [id, load])

  useEffect(() => {
    let objectUrl = ''
    const fileEndpoint = pages.length > 1
      ? `/api/answer-sheets/${id}/pages/${currentPage}/file`
      : `/api/answer-sheets/${id}/file`
    authenticatedImageUrl(fileEndpoint)
      .then((url) => {
        objectUrl = url
        setImage(url)
      })
      .catch(() => {})
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [id, currentPage, pages.length])

  async function evaluate() {
    setBusy(true)
    setError('')
    try {
      await api.post(`/api/evaluations/${id}`)
      load()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusy(false)
    }
  }

  if (loading && !sheet) {
    return (
      <div className="surface" style={{ minHeight: '300px', display: 'grid', placeItems: 'center' }}>
        <p style={{ color: 'var(--text-muted)' }}>Loading evaluation workspace...</p>
      </div>
    )
  }

  if (error && !sheet) {
    return <ErrorState title="Unable to load this answer sheet" message={error} onRetry={load} />
  }

  const isProcessing = ['UPLOADED', 'PROCESSING'].includes(sheet?.status)

  return (
    <div className="evaluation-workspace">
      {/* Hero Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '14px' }}>
        <div>
          <span className="eyebrow">SUBMISSION INSPECTION WORKSPACE</span>
          <h1 style={{ font: '800 32px Manrope', margin: '6px 0' }}>{sheet?.studentName || 'Student Submission'}</h1>
          <p style={{ color: 'var(--text-muted)', margin: 0 }}>
            {sheet?.originalFileName} ·{' '}
            <span className={`tag ${isProcessing ? '' : 'success'}`}>{sheet?.status}</span>
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          {canReview && sheet?.status !== 'PROCESSED' && (
            <button className="btn-netflix-red" onClick={evaluate} disabled={busy}>
              <SparklesIcon size={16} />
              <span>{busy ? 'Running CV + OCR + NLP...' : 'Run AI Evaluation'}</span>
            </button>
          )}
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <QualityPanel sheet={sheet} />

      {/* Split-Screen Workspace */}
      <div className="workspace-grid">
        {/* Left: Document Viewport */}
        <section className="scan-view">
          <div className="viewer-toolbar">
            <div className="toolbar-group">
              <button
                className={`ghost-btn ${activeTab === 'image' ? 'active' : ''}`}
                style={{ padding: '6px 12px', fontSize: '12px' }}
                onClick={() => setActiveTab('image')}
              >
                Document Scan
              </button>
              <button
                className={`ghost-btn ${activeTab === 'ocr' ? 'active' : ''}`}
                style={{ padding: '6px 12px', fontSize: '12px' }}
                onClick={() => setActiveTab('ocr')}
              >
                Extracted Text
              </button>
            </div>

            {pages.length > 1 && (
              <div className="toolbar-group" style={{ background: 'rgba(255,255,255,0.05)', padding: '2px 8px', borderRadius: '8px' }}>
                <button
                  className="rail-nav-btn"
                  style={{ width: '28px', height: '28px', fontSize: '11px' }}
                  onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                  disabled={currentPage <= 1}
                  title="Previous Page"
                >
                  ◀
                </button>
                <span style={{ fontSize: '11.5px', fontWeight: 700, margin: '0 8px', color: '#fff' }}>
                  Page {currentPage} of {pages.length}
                </span>
                <button
                  className="rail-nav-btn"
                  style={{ width: '28px', height: '28px', fontSize: '11px' }}
                  onClick={() => setCurrentPage((p) => Math.min(pages.length, p + 1))}
                  disabled={currentPage >= pages.length}
                  title="Next Page"
                >
                  ▶
                </button>
              </div>
            )}

            <div className="toolbar-group">
              <button
                className="rail-nav-btn"
                onClick={() => setZoom((z) => Math.max(0.5, z - 0.1))}
                title="Zoom Out"
              >
                <ZoomOutIcon size={16} />
              </button>
              <span style={{ fontSize: '12px', minWidth: '40px', textAlign: 'center' }}>
                {Math.round(zoom * 100)}%
              </span>
              <button
                className="rail-nav-btn"
                onClick={() => setZoom((z) => Math.min(2.5, z + 0.1))}
                title="Zoom In"
              >
                <ZoomInIcon size={16} />
              </button>
              <button
                className="rail-nav-btn"
                onClick={() => setRotation((r) => (r + 90) % 360)}
                title="Rotate 90°"
              >
                <RotateIcon size={16} />
              </button>
              <button
                className={`rail-nav-btn ${invertFilter ? 'active' : ''}`}
                onClick={() => setInvertFilter((v) => !v)}
                title="Toggle High-Contrast / Invert Filter"
              >
                <ContrastIcon size={16} />
              </button>
            </div>
          </div>

          {activeTab === 'image' ? (
            <div className={`image-viewport ${invertFilter ? 'invert-filter' : ''}`}>
              {image ? (
                <img
                  src={image}
                  alt="Scanned Answer Sheet"
                  style={{
                    transform: `scale(${zoom}) rotate(${rotation}deg)`,
                    transition: 'transform 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
                    maxWidth: '100%'
                  }}
                />
              ) : (
                <p style={{ color: 'var(--text-muted)' }}>Document preview unavailable.</p>
              )}
            </div>
          ) : (
            <div
              style={{
                marginTop: '16px',
                padding: '18px',
                background: '#090a0f',
                borderRadius: '12px',
                border: '1px solid var(--line)',
                minHeight: '400px',
                maxHeight: '70vh',
                overflowY: 'auto'
              }}
            >
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', font: '13px/1.7 "JetBrains Mono", monospace', color: '#cbd5e1' }}>
                {sheet?.rawOcrText || 'No OCR text extracted yet.'}
              </pre>
            </div>
          )}
        </section>

        {/* Right: Evaluated Rubric Cards */}
        <section className="workspace-evaluations">
          {(isProcessing || busy) && (
            <ProcessingVisualizer active={true} liveEvent={progressEvent} />
          )}

          {evaluations.length > 0 ? (
            evaluations.map((ev) => (
              <EvaluationCard
                key={ev.id}
                evaluation={ev}
                canReview={canReview}
                onChanged={load}
              />
            ))
          ) : !isProcessing ? (
            <div className="surface empty-luxe">
              <p>No evaluation records generated yet. Click "Run AI Evaluation" to begin.</p>
            </div>
          ) : null}
        </section>
      </div>
    </div>
  )
}