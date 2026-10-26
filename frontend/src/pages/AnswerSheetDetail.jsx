import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'
import { authenticatedImageUrl } from '../utils/fileUrl.js'
import ProcessingVisualizer from '../components/ProcessingVisualizer.jsx'
import { ErrorState } from '../components/AsyncState.jsx'
import { useEvaluationSocket } from '../api/useEvaluationSocket.js'

function EvaluationCard({ evaluation, canReview, onChanged }) {
  const [editing, setEditing] = useState(false)
  const [marks, setMarks] = useState(evaluation.marksAwarded)
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const confidence = Math.round((evaluation.overallConfidence || 0) * 100)

  async function review() {
    setBusy(true); setError('')
    try { await api.post(`/api/evaluations/${evaluation.id}/review`, { action: 'APPROVE' }); onChanged() } catch (cause) { setError(cause.message) } finally { setBusy(false) }
  }

  async function override(event) {
    event.preventDefault(); setBusy(true); setError('')
    try { await api.post(`/api/evaluations/${evaluation.id}/override`, { finalMarks: Number(marks), overrideReason: reason || 'Teacher adjustment' }); setEditing(false); onChanged() } catch (cause) { setError(cause.message) } finally { setBusy(false) }
  }

  return <article className="evaluation-card"><div className="eval-head"><div className="q-index">Q{evaluation.questionNumber}</div><div><h3>Question {evaluation.questionNumber}</h3><span>{evaluation.maxMarks} maximum marks</span></div><div className="score-big"><b>{evaluation.finalMarks ?? evaluation.marksAwarded}</b><small>/ {evaluation.maxMarks}</small></div><span className="tag">{confidence}% confidence</span></div><div className="eval-grid"><div><span className="eyebrow">EXTRACTED ANSWER</span><p className="answer-text">{evaluation.studentAnswerText || 'No OCR text returned.'}</p><div className="metric-row"><span>Semantic similarity <b>{Math.round((evaluation.semanticSimilarity || 0) * 100)}%</b></span><span>Concept coverage <b>{Math.round((evaluation.conceptCoverage || 0) * 100)}%</b></span><span>OCR confidence <b>{Math.round(evaluation.ocrConfidence || 0)}%</b></span></div></div><div><span className="eyebrow">CONCEPT EVIDENCE</span><div className="pills">{(evaluation.matchedConcepts || []).map((concept) => <span className="tag success" key={`matched-${concept}`}>✓ {concept}</span>)}{(evaluation.missingConcepts || []).map((concept) => <span className="tag" key={`missing-${concept}`}>− {concept}</span>)}{(evaluation.contradictedConcepts || []).map((concept) => <span className="tag danger" key={`contradicted-${concept}`}>! {concept}</span>)}</div><div className="feedback"><b>AI feedback</b><p>{evaluation.feedback || 'No feedback returned.'}</p></div></div></div>{error && <div className="error-banner">{error}</div>}{canReview && <div className="review-bar"><span className="tag">{evaluation.status}</span><div><button className="ghost-btn small" onClick={review} disabled={busy}>Approve</button><button className="lux-btn small" onClick={() => setEditing(!editing)}>{editing ? 'Close' : 'Override marks'}</button></div></div>}{editing && <form className="override-form" onSubmit={override}><input type="number" min="0" max={evaluation.maxMarks} step="0.5" value={marks} onChange={(event) => setMarks(event.target.value)} required/><input value={reason} onChange={(event) => setReason(event.target.value)} placeholder="Reason for adjustment" required/><button className="lux-btn" disabled={busy}>{busy ? 'Saving...' : 'Save override'}</button></form>}</article>
}

export default function AnswerSheetDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const canReview = ['TEACHER', 'ADMIN'].includes(user?.role)
  const [sheet, setSheet] = useState(null)
  const [evaluations, setEvaluations] = useState([])
  const [image, setImage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [zoom, setZoom] = useState(1)
  const [rotation, setRotation] = useState(0)

  const load = useCallback(() => {
    setError('')
    setLoading(true)
    Promise.all([
      api.get(`/api/answer-sheets/${id}`),
      api.get('/api/evaluations', { params: { answerSheetId: id } })
    ])
      .then(([sheetResponse, evalResponse]) => {
        setSheet(sheetResponse.data?.data || sheetResponse.data)
        setEvaluations(evalResponse.data?.data || evalResponse.data || [])
      })
      .catch((cause) => setError(cause.message || 'Unable to load this answer sheet.'))
      .finally(() => setLoading(false))
  }, [id])

  const { progressEvent } = useEvaluationSocket(id, () => {
    load()
  })

  useEffect(() => {
    load()
    let objectUrl = ''
    authenticatedImageUrl(`/api/answer-sheets/${id}/file`).then((url) => { objectUrl = url; setImage(url) }).catch(() => {})
    return () => objectUrl && URL.revokeObjectURL(objectUrl)
  }, [id, load])

  async function evaluate() {
    setBusy(true); setError('')
    try { await api.post(`/api/evaluations/${id}`); load() } catch (cause) { setError(cause.message) } finally { setBusy(false) }
  }

  if (loading && !sheet) return <div className="detail-loading surface"><div className="skeleton-line wide"/><div className="skeleton-line"/><div className="skeleton-chart"/></div>
  if (error && !sheet) return <ErrorState title="Unable to load this answer sheet" message={error} onRetry={load} />
  const processing = ['UPLOADED', 'PROCESSING'].includes(sheet?.status)
  const qualityPanel = sheet?.imageQualityScore == null ? null : <section className="surface quality-panel"><div className="section-title"><div><span className="eyebrow">IMAGE QUALITY</span><h2>{sheet.imageQualityWarnings?.length ? 'Manual review recommended' : 'Image quality looks readable'}</h2></div><span className={`tag ${sheet.imageQualityWarnings?.length ? 'danger' : 'success'}`}>{Math.round(sheet.imageQualityScore)} / 100</span></div><div className="quality-metrics"><span>Blur <b>{Math.round(sheet.imageBlurScore)}%</b></span><span>Brightness <b>{Math.round(sheet.imageBrightnessScore)}%</b></span><span>Contrast <b>{Math.round(sheet.imageContrastScore)}%</b></span><span>Resolution <b>{sheet.imageResolutionWidth} × {sheet.imageResolutionHeight}</b></span></div>{sheet.imageQualityWarnings?.length > 0 && <ul className="quality-warnings">{sheet.imageQualityWarnings.map((warning) => <li key={warning}>{warning}</li>)}</ul>}</section>

  return <div><div className="detail-hero"><div><span className="eyebrow">SUBMISSION REVIEW</span><h1 className="page-title">{sheet?.studentName}</h1><p className="lead">{sheet?.originalFileName} · <span className={`tag ${processing ? '' : 'success'}`}>{sheet?.status}</span></p></div>{canReview && sheet?.status !== 'PROCESSED' && <button className="lux-btn" onClick={evaluate} disabled={busy}>{busy ? 'Running CV + OCR + NLP...' : 'Run AI evaluation →'}</button>}</div>{error && <div className="error-banner">{error} <button className="inline-action" onClick={load}>Retry</button></div>}{qualityPanel}<div className="review-layout"><section><div className="surface scan-view"><div className="section-title"><div><span className="eyebrow">ORIGINAL EVIDENCE</span><h2>Answer sheet</h2></div><span className="mini-badge">IMMUTABLE SOURCE</span></div><div className="viewer-toolbar" aria-label="Document viewer controls"><button className="icon-btn" onClick={() => setZoom((value) => Math.max(0.5, value - 0.1))} aria-label="Zoom out" title="Zoom out">−</button><span aria-live="polite">{Math.round(zoom * 100)}%</span><button className="icon-btn" onClick={() => setZoom((value) => Math.min(2, value + 0.1))} aria-label="Zoom in" title="Zoom in">+</button><button className="ghost-btn small" onClick={() => { setZoom(1); setRotation(0) }}>Reset view</button><button className="ghost-btn small" onClick={() => setRotation((value) => (value + 90) % 360)}>Rotate</button></div>{image ? <div className="image-viewport"><img src={image} alt="Uploaded answer sheet" style={{ transform: `scale(${zoom}) rotate(${rotation}deg)` }} /></div> : <div className="empty-luxe">Preview unavailable. You can still evaluate the uploaded sheet.</div>}</div><div className="surface"><div className="section-title"><div><span className="eyebrow">OCR LAYER</span><h2>Extracted text</h2></div></div><pre className="ocr-text">{sheet?.rawOcrText || 'OCR has not run yet.'}</pre></div></section><section className="evaluation-stack">{(processing || busy) && <ProcessingVisualizer active={true} liveEvent={progressEvent} />}{evaluations.length ? evaluations.map((evaluation) => <EvaluationCard key={evaluation.id} evaluation={evaluation} canReview={canReview} onChanged={load} />) : !processing && <div className="surface empty-luxe">No evaluation records yet.</div>}</section></div></div>
}