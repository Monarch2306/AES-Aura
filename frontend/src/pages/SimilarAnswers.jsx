import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api from '../api/client'
import { EmptyState, ErrorState } from '../components/AsyncState.jsx'

export default function SimilarAnswers() {
  const { examId } = useParams()
  const [pairs, setPairs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')

  async function review(id, action) {
    const notes = window.prompt('Add review notes')
    if (notes === null) return
    try {
      const reviewNotes = notes.trim() || null
      await api.post(`/api/exams/similar-answers/${id}/${action}`, { notes: reviewNotes })
      setPairs((current) => current.map((pair) => pair.id === id ? { ...pair, status: action === 'dismiss' ? 'DISMISSED' : 'CONFIRMED_BY_REVIEWER', reviewNotes } : pair))
    } catch (cause) { setActionError(cause.message || 'Unable to save review action.') }
  }

  useEffect(() => {
    api.get(`/api/exams/${examId}/similar-answers`).then((response) => setPairs(response.data.data || []))
      .catch((cause) => setError(cause.message || 'Unable to load similar-answer review data.'))
      .finally(() => setLoading(false))
  }, [examId])

  return <div><div className="detail-hero"><div><span className="eyebrow">REVIEW SUPPORT</span><h1 className="page-title">Potentially similar answers</h1><p className="lead">Similarity is a review signal only. It does not prove plagiarism.</p></div></div>{actionError && <div className="error-banner">{actionError}</div>}{loading && <div className="surface async-state"><div className="skeleton-line wide"/><p className="muted">Comparing evaluated answers...</p></div>}{error && <ErrorState title="Similarity review unavailable" message={error}/>} {!loading && !error && !pairs.length && <EmptyState title="No potentially similar answers" message="No answer pairs crossed the configured review threshold."/>}{!loading && !error && pairs.length > 0 && <div className="similarity-list">{pairs.map((pair) => <article className="surface similarity-row" key={`${pair.firstEvaluationId}-${pair.secondEvaluationId}`}><div className="similarity-header"><div><span className="eyebrow">QUESTION {pair.questionNumber}</span><h2>{Math.round(pair.similarity * 100)}% similarity</h2><small className="muted">Text {Math.round(pair.textSimilarity * 100)}% · Concepts {Math.round(pair.conceptSimilarity * 100)}%</small></div><span className={`tag ${pair.severity === 'HIGH' ? 'danger' : ''}`}>{pair.status}</span></div><div className="similarity-answers"><div><b>Student {pair.firstStudentId}</b><p>{pair.firstAnswer}</p></div><div><b>Student {pair.secondStudentId}</b><p>{pair.secondAnswer}</p></div></div><small className="muted">{pair.notice}</small>{pair.status === 'REVIEW_REQUIRED' && <div className="review-bar"><button className="ghost-btn small" onClick={() => review(pair.id, 'dismiss')}>Dismiss</button><button className="lux-btn small" onClick={() => review(pair.id, 'confirm')}>Confirm after review</button></div>}</article>)}</div>}</div>
}
