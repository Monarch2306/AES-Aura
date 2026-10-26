import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api from '../api/client'
import { EmptyState, ErrorState } from '../components/AsyncState.jsx'

export default function ConsistencyReview() {
  const { examId } = useParams()
  const [flags, setFlags] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    api.get(`/api/exams/${examId}/consistency-flags`).then((response) => setFlags(response.data.data || []))
      .catch((cause) => setError(cause.message || 'Unable to load consistency flags.')).finally(() => setLoading(false))
  }, [examId])
  async function resolve(id, action) {
    const notes = window.prompt('Add review notes')
    if (notes === null) return
    try {
      const reviewNotes = notes.trim() || null
      await api.post(`/api/exams/consistency-flags/${id}/${action}`, { notes: reviewNotes })
      setFlags((current) => current.map((flag) => flag.id === id ? { ...flag, status: action === 'dismiss' ? 'DISMISSED' : 'RESOLVED', reviewNotes } : flag))
    } catch (cause) {
      setError(cause.message || 'Unable to save review action.')
    }
  }
  return <div><div className="detail-hero"><div><span className="eyebrow">REVIEW SUPPORT</span><h1 className="page-title">Potential scoring inconsistencies</h1><p className="lead">These flags identify unusual scoring patterns; they do not prove evaluator bias or error.</p></div></div>{loading && <div className="surface async-state"><div className="skeleton-line wide"/><p className="muted">Comparing evaluated answers...</p></div>}{error && <ErrorState title="Consistency review unavailable" message={error}/>} {!loading && !error && !flags.length && <EmptyState title="No consistency flags" message="No evaluated answer pairs crossed the configured review thresholds."/>}{!loading && !error && flags.length > 0 && <div className="similarity-list">{flags.map((flag) => <article className="surface similarity-row" key={flag.id}><div className="similarity-header"><div><span className="eyebrow">QUESTION {flag.questionNumber}</span><h2>{flag.marksDifference} mark difference</h2><small className="muted">Semantic similarity {Math.round(flag.semanticSimilarity * 100)}% · Concept difference {Math.round(flag.conceptCoverageDifference * 100)}%</small></div><span className={`tag ${flag.severity === 'HIGH' ? 'danger' : ''}`}>{flag.status}</span></div><div className="similarity-answers"><div><b>{flag.evaluationMarks} marks</b><p>{flag.evaluationAnswer || 'Answer text unavailable.'}</p></div><div><b>{flag.comparedEvaluationMarks} marks</b><p>{flag.comparedEvaluationAnswer || 'Answer text unavailable.'}</p></div></div><p>{flag.reason}</p>{flag.status === 'REVIEW_REQUIRED' && <div className="review-bar"><button className="ghost-btn small" onClick={() => resolve(flag.id, 'dismiss')}>Dismiss</button><button className="lux-btn small" onClick={() => resolve(flag.id, 'resolve')}>Keep current scores</button></div>}</article>)}</div>}</div>
}
